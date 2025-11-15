package academic_festival.gyeonggi_go.Home.Controller;

import academic_festival.gyeonggi_go.Home.Dto.HomePlaceDto; // DTO 변경
import academic_festival.gyeonggi_go.Home.Dto.UserLocationRequest;
import academic_festival.gyeonggi_go.Home.Service.HomeService;
import academic_festival.gyeonggi_go.Home.Service.TranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HomeController {

    private final HomeService homeService;
    private final TranslationService translationService;
    private final ObjectMapper objectMapper;

    // ObjectMapper를 다시 주입받아 JSON 직렬화에 사용
    public HomeController(HomeService homeService, TranslationService translationService, ObjectMapper objectMapper) {
        this.homeService = homeService;
        this.translationService = translationService;
        this.objectMapper = objectMapper;
        this.objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    @PostMapping("/home")
    // 반환 타입을 Mono<String>으로 유지하여 번역 서비스와 통합
    public Mono<String> getNearestTourData(@RequestBody UserLocationRequest locationRequest) {

        double userLat = locationRequest.getY(); // 위도 (Latitude, Y)
        double userLon = locationRequest.getX(); // 경도 (Longitude, X)

        // HomeService에서 HomePlaceDto 리스트를 가져옴
        List<HomePlaceDto> nearestData = homeService.getNearestTourData(userLat, userLon); // <-- DTO 반환

        Map<String, Object> innerDataMap = new LinkedHashMap<>();
        innerDataMap.put("placeList", nearestData); // 리스트를 placeList 키의 값으로 설정

        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("code", 200);
        responseMap.put("message", "명소 목록 조회 성공");
        responseMap.put("data", innerDataMap); // placeList를 포함하는 맵을 data로 설정

        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            // JSON 매핑 오류 발생 시 500 응답 반환
            return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping for home data.\"}");
        }

        // 번역 서비스 호출
        return translationService.translateToEnglish(jsonString);
    }
}