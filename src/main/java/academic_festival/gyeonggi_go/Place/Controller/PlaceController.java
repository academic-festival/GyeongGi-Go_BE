package academic_festival.gyeonggi_go.Place.Controller;

import academic_festival.gyeonggi_go.Place.Dto.ApiResponse;
import academic_festival.gyeonggi_go.Place.Dto.PlaceDetailDto;
import academic_festival.gyeonggi_go.Place.Service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/places") // API 기본 경로 설정
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<?>> getPlaceDetail(@PathVariable("placeId") Long placeId) {

        try {
            PlaceDetailDto detailDto = placeService.getPlaceDetail(placeId);
            return ResponseEntity.ok(ApiResponse.success(detailDto));

        } catch (NoSuchElementException e) {
            // 명소를 찾지 못한 경우
            ApiResponse<Object> errorResponse = ApiResponse.error(404, "ID " + placeId + "에 해당하는 명소를 찾을 수 없습니다.");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            // 기타 서버 오류
            ApiResponse<Object> errorResponse = ApiResponse.error(500, "명소 상세정보 조회 중 서버 오류 발생");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}