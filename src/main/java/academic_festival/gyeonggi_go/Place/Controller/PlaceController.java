package academic_festival.gyeonggi_go.Place.Controller;

import academic_festival.gyeonggi_go.Home.Service.TranslationService;
import academic_festival.gyeonggi_go.Place.Dto.ApiResponse;
import academic_festival.gyeonggi_go.Place.Dto.PlaceDetailDto;
import academic_festival.gyeonggi_go.Place.Service.PlaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/places") // API 기본 경로 설정
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final TranslationService translationService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{placeId}")
    public Mono<String> getPlaceDetail(@PathVariable("placeId") Long placeId) {
        Mono<ApiResponse<?>> apiResponseMono = Mono.fromCallable(() -> {
            try {
                PlaceDetailDto detailDto = placeService.getPlaceDetail(placeId);
                return ApiResponse.success(detailDto); // 성공 응답
            } catch (NoSuchElementException e) {
                // 명소를 찾지 못한 경우
                return ApiResponse.error(404, "ID " + placeId + "에 해당하는 명소를 찾을 수 없습니다."); // 404 응답
            } catch (Exception e) {
                // 기타 서버 오류
                return ApiResponse.error(500, "명소 상세정보 조회 중 서버 오류 발생"); // 500 응답
            }
        });

        return apiResponseMono
                .flatMap(apiResponse -> {
                    try {
                        String jsonString = objectMapper.writeValueAsString(apiResponse);
                        return translationService.translateToEnglish(jsonString); // 번역 서비스 호출
                    } catch (Exception e) {
                        System.err.println("JSON 매핑 오류: " + e.getMessage());
                        return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping.\"}");
                    }
                });
    }
}