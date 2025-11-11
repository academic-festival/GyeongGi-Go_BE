package academic_festival.gyeonggi_go.Home.Service;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GgApiService {
    private final WebClient.Builder webClientBuilder;
    private final GyeonggiApiKeyService keyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GgApiService(WebClient.Builder webClientBuilder, GyeonggiApiKeyService keyService) {
        this.webClientBuilder = webClientBuilder;
        this.keyService = keyService;
    }

    public List<GgApiResponse.Row> fetchAllTourDataByAllKeys() {
        Set<GgApiResponse.Row> finalCombinedSet = new HashSet<>();
        List<String> allKeys = keyService.getAllApiKeys();
        List<String> allUrls = keyService.getAllApiUrls();

        for (int index = 0; index < allKeys.size(); index++) {
            String currentKey = allKeys.get(index).trim();
            String currentUrl = allUrls.get(index).trim();
            WebClient webClient = webClientBuilder.baseUrl(currentUrl).build();

            System.out.println("\n--- [키 순환 시작] Key Index: " + (index + 1) + ", URL: " + currentUrl + ", Key Prefix: " + currentKey.substring(0, 8) + "... ---");
            List<GgApiResponse.Row> currentKeyRows = loadAllPagesWithKeyAndUrl(webClient, currentKey, currentUrl);
            finalCombinedSet.addAll(currentKeyRows);
            System.out.println("--- [키 순환 완료] 로드된 데이터: " + currentKeyRows.size() + "건, 누적 유니크 총합: " + finalCombinedSet.size() + "건 ---");
        }

        System.out.println("\n5개 API 사용 후 최종 로드된 관광지 총합 (중복 제거 후): " + finalCombinedSet.size() + "건");
        return new ArrayList<>(finalCombinedSet);
    }

    private List<GgApiResponse.Row> loadAllPagesWithKeyAndUrl(WebClient webClient, String serviceKey, String serviceUrl) {
        List<GgApiResponse.Row> allRows = new ArrayList<>();
        int pageSize = 100;
        Integer totalCount = 0; // Integer로 변경하여 null 체크 가능하게 함
        int totalPages = 0;

        try {
            String firstPageJson = fetchTourDataInternal(webClient, 1, pageSize, serviceKey);
            if (firstPageJson == null || firstPageJson.isEmpty()) {
                return allRows;
            }

            GgApiResponse response = objectMapper.readValue(firstPageJson, GgApiResponse.class);
            String serviceName = serviceUrl.substring(serviceUrl.lastIndexOf('/') + 1);

            // combinedData가 비어 있다면, 루트 레벨의 오류 응답(RESULT) 확인
            if (response.getCombinedData().isEmpty()) {
                if (response.getResultOnly() != null) {
                    String resultCode = response.getResultOnly().getCode();
                    String message = response.getResultOnly().getMessage();
                    System.err.println("[" + serviceName + "] API 호출 실패 (루트 RESULT)! CODE: " + resultCode + ", MESSAGE: " + message);
                    return allRows;
                }

                System.err.println("[" + serviceName + "] 응답에 유효한 데이터 또는 오류 구조가 없습니다. (API 구조 불일치)");
                return allRows;
            }


            GgApiResponse.GgApiData apiData = response.getCombinedData().get(0);

            // Head 목록이 비어 있는지 확인
            if (apiData.getHead() == null || apiData.getHead().isEmpty()) {
                System.err.println("[" + serviceName + "] 응답은 받았으나, Head 목록이 비어 있습니다.");
                return allRows;
            }

            GgApiResponse.Head head = apiData.getHead().get(0);

            // Head 객체에 totalCount가 없으면 데이터가 없거나 유효하지 않음
            if (head.getListTotalCount() == null) {
                System.err.println("[" + serviceName + "] Head에는 존재하나 totalCount가 null입니다. (데이터 없음 또는 구조 오류)");
                return allRows;
            }

            totalCount = head.getListTotalCount();

            // RESULT 객체 유무로 성공/실패 판단
            if (head.getResult() != null) {
                // RESULT 객체가 존재하면 실패 메시지(INFO-000이 아닌 경우)를 확인
                String resultCode = head.getResult().getCode();

                if (!"INFO-000".equals(resultCode)) {
                    String message = head.getResult().getMessage();
                    System.err.println("[" + serviceName + "] API 호출 실패! CODE: " + resultCode + ", MESSAGE: " + message);
                    return allRows;
                }

            }

            if (totalCount == 0) {
                System.out.println("[" + serviceName + "] totalCount가 0이므로 로드할 데이터가 없습니다.");
                return allRows;
            }

            totalPages = (int) Math.ceil((double) totalCount / pageSize);

            System.out.println("[" + serviceName + "] 전체 관광지 수: " + totalCount + ", 총 페이지 수: " + totalPages);

            // 첫 페이지의 Row 데이터 추가 (인덱스 1에 데이터가 있을 경우)
            if (response.getCombinedData().size() > 1 && response.getCombinedData().get(1).getRow() != null) {
                allRows.addAll(response.getCombinedData().get(1).getRow());
            }

            // 나머지 페이지 순회
            for (int pageIndex = 2; pageIndex <= totalPages; pageIndex++) {
                String pageJson = fetchTourDataInternal(webClient, pageIndex, pageSize, serviceKey);
                if (pageJson == null || pageJson.isEmpty()) continue;

                GgApiResponse pageResponse = objectMapper.readValue(pageJson, GgApiResponse.class);

                if (pageResponse.getCombinedData() != null && pageResponse.getCombinedData().size() > 1) {
                    List<GgApiResponse.Row> rows = pageResponse.getCombinedData().get(1).getRow();
                    if (rows != null) {
                        allRows.addAll(rows);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("데이터 로딩 중 문제 발생 (URL: " + serviceUrl + "): " + e.getMessage());
            e.printStackTrace();
        }
        return allRows;
    }

    private String fetchTourDataInternal(WebClient webClient, int pageIndex, int pageSize, String serviceKey) {
        try {
            String finalKey = serviceKey;

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("KEY", finalKey)
                            .queryParam("Type", "json")
                            .queryParam("pIndex", pageIndex)
                            .queryParam("pSize", pageSize)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        System.err.println("WebClient 오류 발생! HTTP Status: " + clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("Error Body: " + errorBody);
                                    return Mono.error(new RuntimeException("API 응답 오류, Status: " + clientResponse.statusCode()));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.err.println("API 호출 중 오류 발생 (키: " + serviceKey.substring(0, 8) + "...): " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
}