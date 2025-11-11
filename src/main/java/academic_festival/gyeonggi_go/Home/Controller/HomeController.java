package academic_festival.gyeonggi_go.Home.Controller;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Dto.UserLocationRequest;
import academic_festival.gyeonggi_go.Home.Service.HomeService;
import academic_festival.gyeonggi_go.Home.Service.TranslationService;
import com.fasterxml.jackson.databind.ObjectMapper; // ObjectMapper 재사용
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
    private final ObjectMapper objectMapper; // ObjectMapper 필드 추가

    public HomeController(HomeService homeService, TranslationService translationService, ObjectMapper objectMapper) {
        this.homeService = homeService;
        this.translationService = translationService;
        this.objectMapper = objectMapper; // 생성자에서 주입
    }

    @PostMapping("/home")
    // 반환 타입을 Mono<Map<String, Object>>에서 Mono<String>으로 변경
    public Mono<String> getNearestTourData(@RequestBody UserLocationRequest locationRequest) {

        double lat = locationRequest.getX();
        double lon = locationRequest.getY();

        List<GgApiResponse.Row> nearestData = homeService.getNearestTourData(lat, lon);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("code", 200);
        responseMap.put("message", "명소 목록 조회 성공");
        responseMap.put("data", nearestData);

        // Map 객체를 JSON 문자열로 변환하고 TranslationService 호출
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            // JSON 변환 실패 시 오류 메시지 반환
            return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping for translation.\"}");
        }

        // TranslationService를 통해 번역된 JSON 문자열 Mono 반환
        return translationService.translateToEnglish(jsonString);
    }
}