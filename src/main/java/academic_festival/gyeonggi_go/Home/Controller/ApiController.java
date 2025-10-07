package academic_festival.gyeonggi_go.Home.Controller;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Service.GgApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApiController {
    private final GgApiService ggApiService;

    public ApiController(GgApiService ggApiService) {
        this.ggApiService = ggApiService;
    }

    @GetMapping("/tour/load-all")
    public List<GgApiResponse.Row> loadAllHtstData() {
        return ggApiService.fetchAllTourDataByAllKeys();
    }
}