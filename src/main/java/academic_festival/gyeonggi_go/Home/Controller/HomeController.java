package academic_festival.gyeonggi_go.Home.Controller;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Dto.UserLocationRequest; // 추가
import academic_festival.gyeonggi_go.Home.Service.HomeService;
import academic_festival.gyeonggi_go.Home.Service.TranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping; // POST 요청으로 변경
import org.springframework.web.bind.annotation.RequestBody; // Request Body 사용
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

    public HomeController(HomeService homeService, TranslationService translationService, ObjectMapper objectMapper) {
        this.homeService = homeService;
        this.translationService = translationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/home")
    public Mono<String> getNearestTourData(@RequestBody UserLocationRequest locationRequest) {

        double lat = locationRequest.getX();
        double lon = locationRequest.getY();

        // HomeService를 호출하여 거리 순으로 정렬된 데이터 받기
        List<GgApiResponse.Row> nearestData = homeService.getNearestTourData(lat, lon);

        // Map으로 구성하여 반환
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("code", 200);
        responseMap.put("message", "명소 목록 조회 성공");
        responseMap.put("data", nearestData);

        // Map을 Json 문자열로 변환
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping.\"}");
        }

        return translationService.translateToEnglish(jsonString);
    }

}