package academic_festival.gyeonggi_go.Home.Service;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
        int totalCount;
        int totalPages;

        try {
            String firstPageJson = fetchTourDataInternal(webClient, 1, pageSize, serviceKey);
            if (firstPageJson == null) return allRows;

            GgApiResponse response = objectMapper.readValue(firstPageJson, GgApiResponse.class);

            if (response.getCombinedData() != null && !response.getCombinedData().isEmpty()) {

                totalCount = response.getCombinedData().get(0).getHead().get(0).getListTotalCount();
                totalPages = (int) Math.ceil((double) totalCount / pageSize);

                String serviceName = serviceUrl.substring(serviceUrl.lastIndexOf('/') + 1);
                System.out.println("[" + serviceName + "] 전체 관광지 수: " + totalCount + ", 총 페이지 수: " + totalPages);

                if (response.getCombinedData().size() > 1 && response.getCombinedData().get(1).getRow() != null) {
                    allRows.addAll(response.getCombinedData().get(1).getRow());
                }
            } else {
                System.err.println("API 응답 구조가 유효하지 않습니다. (URL: " + serviceUrl + ")");
                return allRows;
            }

            for (int pageIndex = 2; pageIndex <= totalPages; pageIndex++) {
                String pageJson = fetchTourDataInternal(webClient, pageIndex, pageSize, serviceKey);
                if (pageJson == null) continue;

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
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.err.println("API 호출 중 오류 발생 (키: " + serviceKey.substring(0, 8) + "...): " + e.getMessage());
            return null;
        }
    }
}