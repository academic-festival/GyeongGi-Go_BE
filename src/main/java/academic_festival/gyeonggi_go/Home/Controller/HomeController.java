package academic_festival.gyeonggi_go.Home.Controller;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Dto.UserLocationRequest;
import academic_festival.gyeonggi_go.Home.Service.HomeService;
import academic_festival.gyeonggi_go.Home.Service.TranslationService;
// import com.fasterxml.jackson.databind.ObjectMapper; // <-- 수정 1: 이젠 수동 변환이 필요 없으므로 제거 (또는 냅둬도 됨)
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
    // private final ObjectMapper objectMapper; // <-- 생성자에서 제거해도 됨

    public HomeController(HomeService homeService, TranslationService translationService, ObjectMapper objectMapper) {
        this.homeService = homeService;
        this.translationService = translationService;
        // this.objectMapper = objectMapper;
    }

    @PostMapping("/home")
    // 수정 2: 반환 타입을 Mono<String>에서 Mono<Map<String, Object>>로 변경
    public Mono<Map<String, Object>> getNearestTourData(@RequestBody UserLocationRequest locationRequest) {

        double lat = locationRequest.getX();
        double lon = locationRequest.getY();

        List<GgApiResponse.Row> nearestData = homeService.getNearestTourData(lat, lon);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("code", 200);
        responseMap.put("message", "명소 목록 조회 성공");
        responseMap.put("data", nearestData); // data에 List 객체를 그대로 넣음

        // 수정 3: Map을 수동으로 JSON 문자열로 변환하고 번역하던 코드를 "삭제"
        /*
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping.\"}");
        }
        return translationService.translateToEnglish(jsonString);
        */

        // 수정 3 (대체): Map 객체를 Mono.just()로 감싸서 반환
        // Spring이 알아서 이 Map을 사진 1번과 같은 JSON 객체로 변환해줍니다.
        return Mono.just(responseMap);
    }
}