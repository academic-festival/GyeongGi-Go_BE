package academic_festival.gyeonggi_go.Place.Controller;

import academic_festival.gyeonggi_go.Home.Service.TranslationService;
import academic_festival.gyeonggi_go.Place.Dto.ApiResponse;
import academic_festival.gyeonggi_go.Place.Dto.PlaceDetailDto;
import academic_festival.gyeonggi_go.Place.Service.PlaceService;
import com.fasterxml.jackson.databind.ObjectMapper; // ObjectMapper 재사용
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
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final TranslationService translationService; // TranslationService 재사용
    private final ObjectMapper objectMapper; // ObjectMapper 필드 추가

    @GetMapping("/{placeId}")
    public Mono<String> getPlaceDetail(@PathVariable("placeId") Long placeId) {

        Mono<ApiResponse<?>> apiResponseMono = Mono.fromCallable(() -> {
            try {
                PlaceDetailDto detailDto = placeService.getPlaceDetail(placeId);
                return ApiResponse.success(detailDto); // 성공 응답
            } catch (NoSuchElementException e) {
                // 명소를 찾지 못한 경우
                return ApiResponse.error(404, "ID " + placeId + "에 해당하는 명소를 찾을 수 없습니다.");
            } catch (Exception e) {
                // 기타 서버 오류
                return ApiResponse.error(500, "명소 상세정보 조회 중 서버 오류 발생");
            }
        });

        // Mono 체인 내에서 ApiResponse를 JSON으로 변환 후 번역 서비스 호출
        return apiResponseMono
                .flatMap(apiResponse -> {
                    try {
                        // ApiResponse 객체를 JSON 문자열로 변환
                        String jsonString = objectMapper.writeValueAsString(apiResponse);

                        // TranslationService를 통해 번역 서비스 호출
                        return translationService.translateToEnglish(jsonString);
                    } catch (Exception e) {
                        System.err.println("JSON 매핑 오류: " + e.getMessage());
                        return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping for translation.\"}");
                    }
                });
    }
}