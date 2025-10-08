package academic_festival.gyeonggi_go.Home.Controller;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Dto.UserLocationRequest; // 추가
import academic_festival.gyeonggi_go.Home.Service.HomeService;
import org.springframework.web.bind.annotation.PostMapping; // POST 요청으로 변경
import org.springframework.web.bind.annotation.RequestBody; // Request Body 사용
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @PostMapping("/home")
    public Map<String, Object> getNearestTourData(@RequestBody UserLocationRequest locationRequest) {

        double lat = locationRequest.getX();
        double lon = locationRequest.getY();

        // HomeService를 호출하여 거리 순으로 정렬된 데이터를 받습니다.
        List<GgApiResponse.Row> nearestData = homeService.getNearestTourData(lat, lon);

        // 요청하신 응답 형식에 맞춰 Map으로 구성하여 반환합니다.
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "명소 목록 조회 성공");
        response.put("data", nearestData);

        return response;
    }
}