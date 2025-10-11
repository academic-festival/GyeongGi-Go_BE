package academic_festival.gyeonggi_go.Place.Dto;

import academic_festival.gyeonggi_go.Place.Domain.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlaceDetailDto {
    private String placeName;
    private String placeImg;
    private String locationExplain;
    private String price;
    private String inquiry;

    public PlaceDetailDto(Place place) {
        this.placeName = place.getPlaceName();
        this.placeImg = place.getPlaceImg();
        this.locationExplain = place.getLocationExplain();
        this.price = place.getPrice();
        this.inquiry = place.getInquiry();
    }
}