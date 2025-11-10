package academic_festival.gyeonggi_go.Place.Controller;

import academic_festival.gyeonggi_go.Home.Service.TranslationService;
import academic_festival.gyeonggi_go.Place.Dto.ApiResponse;
import academic_festival.gyeonggi_go.Place.Dto.PlaceDetailDto;
import academic_festival.gyeonggi_go.Place.Service.PlaceService;
// import com.fasterxml.jackson.databind.ObjectMapper; // <-- 수정: 제거
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
    // private final TranslationService translationService; // <-- 수정: 제거
    // private final ObjectMapper objectMapper; // <-- 수정: 제거

    @GetMapping("/{placeId}")
    // 수정: 반환 타입을 Mono<String>에서 Mono<ResponseEntity<ApiResponse<?>>>로 변경
    public Mono<ResponseEntity<ApiResponse<?>>> getPlaceDetail(@PathVariable("placeId") Long placeId) {

        return Mono.fromCallable(() -> {
            try {
                PlaceDetailDto detailDto = placeService.getPlaceDetail(placeId);
                // 수정: ApiResponse를 ResponseEntity.ok()로 감싸서 반환
                return ResponseEntity.ok(ApiResponse.success(detailDto)); // 성공 응답
            } catch (NoSuchElementException e) {
                // 명소를 찾지 못한 경우
                ApiResponse<?> errorResponse = ApiResponse.error(404, "ID " + placeId + "에 해당하는 명소를 찾을 수 없습니다.");
                // 수정: ResponseEntity로 HTTP 404 상태 코드와 함께 반환
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } catch (Exception e) {
                // 기타 서버 오류
                ApiResponse<?> errorResponse = ApiResponse.error(500, "명소 상세정보 조회 중 서버 오류 발생");
                // 수정: ResponseEntity로 HTTP 500 상태 코드와 함께 반환
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        });

        // 수정: flatMap을 이용한 수동 JSON 변환 및 번역 로직 "전부 삭제"
        /*
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
        */
    }
}