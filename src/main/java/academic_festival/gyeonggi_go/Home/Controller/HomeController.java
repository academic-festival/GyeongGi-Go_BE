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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
    }

    @PostMapping("/home")
    // 반환 타입을 Mono<String>으로 유지하여 번역 서비스와 통합
    public Mono<String> getNearestTourData(@RequestBody UserLocationRequest locationRequest) {

        double userLat = locationRequest.getY(); // 위도 (Latitude, Y)
        double userLon = locationRequest.getX(); // 경도 (Longitude, X)

        // HomeService에서 HomePlaceDto 리스트를 가져옴
        List<HomePlaceDto> nearestData = homeService.getNearestTourData(userLat, userLon); // <-- DTO 반환

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("code", 200);
        responseMap.put("message", "명소 목록 조회 성공");
        responseMap.put("data", nearestData); // data에 HomePlaceDto List 객체를 넣음

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