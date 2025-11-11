package academic_festival.gyeonggi_go.Home.Service;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Dto.HomePlaceDto; // DTO 변경
import academic_festival.gyeonggi_go.Place.Domain.Place;
import academic_festival.gyeonggi_go.Place.Repository.PlaceRepository; // 추가
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HomeService {

    private final GgApiService ggApiService;
    private final PlaceRepository placeRepository; // <-- 추가

    public HomeService(GgApiService ggApiService, PlaceRepository placeRepository) { // <-- 생성자 수정
        this.ggApiService = ggApiService;
        this.placeRepository = placeRepository;
    }

    public List<HomePlaceDto> getNearestTourData(double userLat, double userLon) { // <-- 반환 타입 수정
        // GgApiService를 통해 모든 관광지 데이터 로드 (중복 제거된 데이터)
        List<GgApiResponse.Row> allTourData = ggApiService.fetchAllTourDataByAllKeys();

        // 1. 관광지 데이터를 사용자 위치 기준으로 거리 계산 및 정렬 (기존 로직 유지)
        List<GgApiResponse.Row> sortedList = allTourData.stream()
                .filter(row -> row.getRefineWgs84Lat() != null && row.getRefineWgs84Logt() != null)
                .filter(row -> !row.getRefineWgs84Lat().trim().isEmpty() && !row.getRefineWgs84Logt().trim().isEmpty())
                .map(row -> {
                    try {
                        double dataLat = Double.parseDouble(row.getRefineWgs84Lat());
                        double dataLon = Double.parseDouble(row.getRefineWgs84Logt());

                        return new DistanceWrapper(row, calculateEuclideanDistance(userLat, userLon, dataLat, dataLon));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(wrapper -> wrapper.distance))
                .map(wrapper -> wrapper.row)
                .collect(Collectors.toList());

        System.out.println("\n사용자 위치(" + userLat + ", " + userLon + ") 기반, 거리 순으로 " + sortedList.size() + "개의 명소 정렬 완료.");

        // 2. 정렬된 Row 리스트를 HomePlaceDto로 변환하면서 placeId 조회
        List<HomePlaceDto> homePlaceDtos = sortedList.stream()
                .map(row -> {
                    String placeName = row.getTurSmInfoNmForOutput();
                    // PlaceBatchSyncService의 Address 저장 로직을 따름
                    String addressToQuery = Optional.ofNullable(row.getSmReAddr())
                            .filter(a -> !a.isEmpty())
                            .orElse(row.getSigunNm());

                    // DB에서 Place 엔티티 조회 (Name과 Address 기반)
                    Optional<Place> placeOptional = placeRepository.findByPlaceNameAndAddress(placeName, addressToQuery);

                    if (placeOptional.isPresent()) {
                        // DB의 Place 엔티티와 API의 Row 데이터를 결합하여 DTO 생성
                        return new HomePlaceDto(row, placeOptional.get());
                    } else {
                        // DB에 없는 데이터는 필터링
                        return null;
                    }
                })
                .filter(Objects::nonNull) // DB에 존재하는 데이터만 필터링
                .collect(Collectors.toList());

        return homePlaceDtos;
    }

    private double calculateEuclideanDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
    }

    private static class DistanceWrapper {
        final GgApiResponse.Row row;
        final double distance;

        DistanceWrapper(GgApiResponse.Row row, double distance) {
            this.row = row;
            this.distance = distance;
        }
    }
}