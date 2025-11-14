package academic_festival.gyeonggi_go.Home.Dto;

import academic_festival.gyeonggi_go.Place.Domain.Place;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class HomePlaceDto {
    private Long placeId; // Place 고유 ID
    private String placeName;
    private String address;
    private Double x; // 경도 (Longitude)
    private Double y; // 위도 (Latitude)
    private List<String> placeImages; // Place 엔티티의 JSON 직렬화된 URL 리스트

    // 1. 기존 생성자: GgApiResponse.Row와 Place 엔티티를 받아 DTO 생성
    public HomePlaceDto(GgApiResponse.Row row, Place place) {
        // DB 데이터
        this.placeId = place.getPlaceId();

        // Place 엔티티에서 이미지 리스트 추출 (JSON 직렬화 사용)
        this.placeImages = place.getPlaceImg() != null ?
                place.getPlaceImg().stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList()) :
                List.of();

        // Gyeonggi API Data (HomeService에서 DB 매핑 실패 시 사용할 수 있으므로 유지)
        this.placeName = row.getTurSmInfoNmForOutput();
        // PlaceBatchSyncService의 Address 저장 로직을 따름
        this.address = row.getSmReAddr() != null && !row.getSmReAddr().isEmpty() ?
                row.getSmReAddr() :
                row.getSigunNm();

        try {
            // 위도 (y) / 경도 (x)
            this.y = Double.parseDouble(row.getRefineWgs84Lat());
            this.x = Double.parseDouble(row.getRefineWgs84Logt());
        } catch (NumberFormatException e) {
            this.x = null;
            this.y = null;
        }
    }

    // 2. 새 생성자: Place 엔티티만을 받아 DTO를 생성 (HomeService 성능 개선용)
    public HomePlaceDto(Place place) {
        this.placeId = place.getPlaceId();
        this.placeName = place.getPlaceName();
        this.address = place.getAddress();
        this.y = place.getY(); // 위도 (Latitude)
        this.x = place.getX(); // 경도 (Longitude)

        // Place 엔티티에서 이미지 리스트 추출
        this.placeImages = place.getPlaceImg() != null ?
                place.getPlaceImg().stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList()) :
                List.of();
    }
}