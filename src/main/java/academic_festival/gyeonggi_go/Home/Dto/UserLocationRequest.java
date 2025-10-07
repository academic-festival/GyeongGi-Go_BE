package academic_festival.gyeonggi_go.Home.Dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLocationRequest {
    // 경도 (Longitude, x)
    private double x;

    // 위도 (Latitude, y)
    private double y;
}