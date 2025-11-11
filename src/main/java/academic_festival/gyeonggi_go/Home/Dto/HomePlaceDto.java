package academic_festival.gyeonggi_go.Home.Dto;

import academic_festival.gyeonggi_go.Place.Domain.Place;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class HomePlaceDto {
    private Long placeId; // <-- DB 고유 ID 추가
    private String placeName;
    private String address;
    private Double x; // 경도 (Longitude)
    private Double y; // 위도 (Latitude)
    List<String> placeImg; // Place 엔티티에서 가져옴
    private String inquiry;

    // GgApiResponse.Row와 Place 엔티티를 받아 DTO 생성
    public HomePlaceDto(GgApiResponse.Row row, Place place) {
        // DB 데이터
        this.placeId = place.getPlaceId();
        this.placeImg = place.getPlaceImg();

        // Gyeonggi API Data
        this.placeName = row.getTurSmInfoNmForOutput();
        // PlaceBatchSyncService의 주소 저장 로직을 따름
        this.address = row.getSmReAddr() != null && !row.getSmReAddr().isEmpty() ? row.getSmReAddr() : row.getSigunNm();
        this.inquiry = row.getTelNo();

        try {
            this.x = Double.parseDouble(row.getRefineWgs84Logt());
            this.y = Double.parseDouble(row.getRefineWgs84Lat());
        } catch (NumberFormatException e) {
            this.x = null;
            this.y = null;
        }
    }
}