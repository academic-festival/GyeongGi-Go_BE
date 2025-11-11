package academic_festival.gyeonggi_go.Place.Domain;

import academic_festival.gyeonggi_go.config.StringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("관광지 고유 ID (PK)")
    private Long placeId;

    @Column(name = "place_name", length = 255, nullable = false)
    @Comment("이름")
    private String placeName;

    @Column(name = "address", length = 500)
    @Comment("주소")
    private String address;

    @Column(name = "x", columnDefinition = "DECIMAL(15, 12)") // 경도 (Longitude)
    @Comment("경도")
    private Double x;

    @Column(name = "y", columnDefinition = "DECIMAL(15, 12)") // 위도 (Latitude)
    @Comment("위도")
    private Double y;

    @Convert(converter = StringListConverter.class)
    @Column(name = "place_img", columnDefinition = "TEXT")
    @Comment("이미지 URL 리스트 (JSON 직렬화)")
    private List<String> placeImg;

    @Lob
    @Column(name = "location_explain", columnDefinition = "TEXT")
    @Comment("장소 설명")
    private String locationExplain;

    @Column(name = "operating_hours", length = 255)
    @Comment("운영 시간")
    private String operatingHours;

    @Column(name = "price", length = 255)
    @Comment("요금 정보")
    private String price;

    @Column(name = "inquiry", length = 255)
    @Comment("문의 정보")
    private String inquiry;

    @Column(name = "viewing_hours", length = 255)
    @Comment("추천 관람 시간")
    private String viewingHours;

    @Lob
    @Column(name = "chatbot_context", columnDefinition = "LONGTEXT")
    @Comment("챗봇용 상세 컨텍스트")
    private String chatbotContext;
}