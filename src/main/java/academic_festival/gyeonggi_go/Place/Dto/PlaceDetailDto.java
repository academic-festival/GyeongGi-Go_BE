package academic_festival.gyeonggi_go.Place.Dto;

import academic_festival.gyeonggi_go.Place.Domain.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PlaceDetailDto {
    private Long placeId;
    private String placeName;
    private List<String> placeImg;
    private String locationExplain;
    private String price;
    private String inquiry;

    public PlaceDetailDto(Place place) {
        this.placeId = place.getPlaceId();
        this.placeName = place.getPlaceName();
        this.placeImg = place.getPlaceImg();
        this.locationExplain = place.getLocationExplain();
        this.price = place.getPrice();
        this.inquiry = place.getInquiry();
    }
}