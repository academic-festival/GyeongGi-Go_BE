package academic_festival.gyeonggi_go.Place.Service;

import academic_festival.gyeonggi_go.Place.Domain.Place;
import academic_festival.gyeonggi_go.Place.Dto.PlaceDetailDto;
import academic_festival.gyeonggi_go.Place.Repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용 트랜잭션 설정
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceDetailDto getPlaceDetail(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new NoSuchElementException("ID " + placeId + "에 해당하는 명소를 찾을 수 없습니다."));

        // Entity를 DTO로 변환하여 반환
        return new PlaceDetailDto(place);
    }
}