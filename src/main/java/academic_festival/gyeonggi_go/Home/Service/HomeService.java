package academic_festival.gyeonggi_go.Home.Service;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Dto.HomePlaceDto;
import academic_festival.gyeonggi_go.Place.Domain.Place;
import academic_festival.gyeonggi_go.Place.Repository.PlaceRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HomeService {

    private final GgApiService ggApiService;
    private final PlaceRepository placeRepository;

    public HomeService(GgApiService ggApiService, PlaceRepository placeRepository) {
        this.ggApiService = ggApiService;
        this.placeRepository = placeRepository;
    }

    public List<HomePlaceDto> getNearestTourData(double userLat, double userLon) {
        List<GgApiResponse.Row> allTourData = ggApiService.fetchAllTourDataByAllKeys();

        // 1. 거리 계산 및 정렬 로직
        List<GgApiResponse.Row> sortedList = allTourData.stream()
                .filter(row -> row.getRefineWgs84Lat() != null && row.getRefineWgs84Logt() != null)
                .filter(row -> !row.getRefineWgs84Lat().isEmpty() && !row.getRefineWgs84Logt().isEmpty())
                .map(row -> {
                    try {
                        double lat = Double.parseDouble(row.getRefineWgs84Lat());
                        double lon = Double.parseDouble(row.getRefineWgs84Logt());
                        double distance = calculateEuclideanDistance(userLat, userLon, lat, lon);
                        return new DistanceWrapper(row, distance);
                    } catch (NumberFormatException e) {
                        return null; // 숫자 변환 오류 발생 시 제외
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(w -> w.distance))
                .map(w -> w.row)
                .collect(Collectors.toList());

        System.out.println("가장 가까운 순으로 " + sortedList.size() + "개의 명소 정렬 완료.");

        // 2. 정렬된 Row 리스트를 HomePlaceDto로 변환하면서 placeId 조회
        List<HomePlaceDto> homePlaceDtos = sortedList.stream()
                .map(row -> {
                    String placeName = row.getTurSmInfoNmForOutput();

                    Double x = null;
                    Double y = null;

                    try {
                        // 좌표를 다시 파싱하여 DB 조회에 사용
                        y = Double.parseDouble(row.getRefineWgs84Lat());
                        x = Double.parseDouble(row.getRefineWgs84Logt());
                    } catch (NumberFormatException e) {
                        return null; // 좌표 오류 시 매핑 실패 처리
                    }

                    Optional<Place> placeOptional = placeRepository.findByPlaceNameAndXAndY(placeName, x, y);

                    if (placeOptional.isPresent()) {
                        // DB의 Place 엔티티만 사용하여 HomePlaceDto 생성
                        return new HomePlaceDto(placeOptional.get());
                    } else {
                        // DB에 매핑되는 행이 없으면 제외
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println("--- [필터링 시작] 최종 명소 리스트 중 주소에 '수원'이 포함된 명소만 필터링합니다. ---");

        // 3. '수원' 주소 필터링
        List<HomePlaceDto> filteredList = homePlaceDtos.stream()
                // 주소 필드가 null이 아니며 '수원'을 포함하는 경우만 통과
                .filter(dto -> dto.getAddress() != null && dto.getAddress().contains("수원"))
                .collect(Collectors.toList());

        System.out.println("--- [필터링 완료] '수원' 명소 수: " + filteredList.size() + "건 ---");

        // 4. 최종 응답 JSON에서 placeName을 기준으로 중복 제거 (가장 가까운 명소 하나만 남김)
        return filteredList.stream()
                .collect(Collectors.toMap(
                        HomePlaceDto::getPlaceName, // Key: placeName
                        dto -> dto,
                        (existing, replacement) -> existing, // 기존 값(먼저 발견된, 가장 가까운) 유지
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .collect(Collectors.toList());
    }

    private double calculateEuclideanDistance(double lat1, double lon1, double lat2, double lon2) {
        // 유클리드 거리 계산 공식
        return Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
    }

    // 내부 클래스: 정렬을 위해 Row와 거리를 함께 저장
    private static class DistanceWrapper {
        final GgApiResponse.Row row;
        final double distance;

        DistanceWrapper(GgApiResponse.Row row, double distance) {
            this.row = row;
            this.distance = distance;
        }
    }
}