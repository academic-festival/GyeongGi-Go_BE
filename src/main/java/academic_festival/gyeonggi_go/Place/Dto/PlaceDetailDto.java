package academic_festival.gyeonggi_go.Place.Dto;

import academic_festival.gyeonggi_go.Place.Domain.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class PlaceDetailDto {
    private Long placeId;
    private String placeName;
    private String address; // <-- 주소 추가
    private List<String> placeImages; // placeImgUrls로 대체
    private String locationExplain;
    private String price;
    private String inquiry;

    public PlaceDetailDto(Place place) {
        this.placeId = place.getPlaceId();
        this.placeName = place.getPlaceName();
        this.address = place.getAddress(); // <-- 주소 설정 추가
        this.placeImages = place.getPlaceImg() != null ?
                place.getPlaceImg().stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList()) :
                List.of();
        this.locationExplain = place.getLocationExplain();
        this.price = place.getPrice();
        this.inquiry = place.getInquiry();
    }
}