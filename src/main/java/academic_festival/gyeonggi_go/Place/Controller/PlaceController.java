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
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final TranslationService translationService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{placeId}")
    // 반환 타입: 번역된 JSON 문자열을 직접 반환하기 위해 Mono<String> 유지
    public Mono<String> getPlaceDetail(@PathVariable("placeId") Long placeId) {

        // 1. 데이터 조회 및 ApiResponse 생성 Mono
        Mono<ApiResponse<?>> apiResponseMono = Mono.fromCallable(() -> {
            try {
                // placeId가 포함된 PlaceDetailDto를 가져옴
                PlaceDetailDto detailDto = placeService.getPlaceDetail(placeId);
                return ApiResponse.success(detailDto);
            } catch (NoSuchElementException e) {
                return ApiResponse.error(404, "ID " + placeId + "에 해당하는 명소를 찾을 수 없습니다.");
            } catch (Exception e) {
                return ApiResponse.error(500, "명소 상세정보 조회 중 서버 오류 발생");
            }
        });

        // 2. ApiResponse Mono를 JSON으로 변환 후 번역 서비스 호출
        return apiResponseMono
                .flatMap(apiResponse -> {
                    try {
                        String jsonString = objectMapper.writeValueAsString(apiResponse);
                        // TranslationService를 통해 번역 서비스 호출 (placeId는 좌표가 아니므로 번역되지 않고 유지됨)
                        return translationService.translateToEnglish(jsonString);
                    } catch (Exception e) {
                        System.err.println("JSON 매핑 오류: " + e.getMessage());
                        return Mono.just("{\"code\":500, \"message\":\"Internal Server Error during JSON mapping for translation.\"}");
                    }
                });
    }
}