package academic_festival.gyeonggi_go.Home.Controller;

import academic_festival.gyeonggi_go.Home.Dto.HomePlaceDto;
import academic_festival.gyeonggi_go.Home.Dto.UserLocationRequest;
import academic_festival.gyeonggi_go.Home.Service.HomeService;
import academic_festival.gyeonggi_go.Home.Service.TranslationService;
import academic_festival.gyeonggi_go.chatbot.Dto.Request.ChatbotRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Collectors 임포트 필요

@RestController
public class HomeController {

    private final HomeService homeService;
    private final TranslationService translationService;
    private final ObjectMapper objectMapper;

    public HomeController(HomeService homeService, TranslationService translationService, ObjectMapper objectMapper) {
        this.homeService = homeService;
        this.translationService = translationService;
        this.objectMapper = objectMapper;
        this.objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    @PostMapping("/home")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    examples = {
                            @ExampleObject(
                                    value = "{\"x\": 127.0151841956, \"y\": 37.2807973662}"
                            )
                    }
            )
    )
    public Mono<String> getNearestTourData(@RequestBody UserLocationRequest locationRequest) {

        double userLat = locationRequest.getY();
        double userLon = locationRequest.getX();

        List<HomePlaceDto> nearestData = homeService.getNearestTourData(userLat, userLon);

        // 1. 재주입을 위해 placeId와 placeImages 리스트를 저장
        Map<Long, List<String>> originalImageMap = nearestData.stream()
                .collect(Collectors.toMap(
                        HomePlaceDto::getPlaceId,
                        HomePlaceDto::getPlaceImages
                ));

        // 2. 번역을 위해 placeImages 필드를 제외한 임시 리스트를 생성
        List<Map<String, Object>> sanitizedList = nearestData.stream().map(dto -> {
            // DTO를 Map으로 변환
            Map<String, Object> map = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});
            map.remove("placeImages"); // 긴 URL 리스트 제거
            return map;
        }).collect(Collectors.toList());

        // 3. 응답 맵 구성 (URL이 제거된 리스트 사용)
        Map<String, Object> innerDataMap = new LinkedHashMap<>();
        innerDataMap.put("placeList", sanitizedList);

        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("code", 200);
        responseMap.put("message", "명소 목록 조회 성공");
        responseMap.put("data", innerDataMap);

        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(responseMap); // 짧은 JSON 문자열
        } catch (Exception e) {
            return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping for home data.\"}");
        }

        // 4. 번역 서비스 호출 및 후처리 (원본 URL 재주입)
        return translationService.translateToEnglish(jsonString)
                .map(translatedJson -> {
                    try {
                        // 번역된 JSON을 파싱
                        Map<String, Object> translatedResponse = objectMapper.readValue(translatedJson, new TypeReference<Map<String, Object>>() {});
                        Map<String, Object> translatedData = (Map<String, Object>) translatedResponse.get("data");
                        List<Map<String, Object>> translatedPlaceList = (List<Map<String, Object>>) translatedData.get("placeList");

                        // placeId를 기준으로 원본 URL 재주입
                        translatedPlaceList.forEach(item -> {
                            // placeId는 Long 타입이므로 안전하게 처리
                            Long placeId = ((Number) item.get("placeId")).longValue();
                            if (originalImageMap.containsKey(placeId)) {
                                item.put("placeImages", originalImageMap.get(placeId));
                            }
                        });

                        return objectMapper.writeValueAsString(translatedResponse); // 최종 JSON 반환
                    } catch (Exception e) {
                        System.err.println("번역된 JSON에 URL 재주입 실패: " + e.getMessage());
                        // 재주입 실패 시, 번역된 텍스트만이라도 반환 (URL은 누락됨)
                        return translatedJson;
                    }
                })
                .onErrorResume(e -> {
                    // 번역 API 호출 실패 시 원본 한국어 JSON 반환
                    System.err.println("번역 실패, 원본 JSON 반환: " + e.getMessage());
                    return Mono.just(jsonString);
                });
    }
}