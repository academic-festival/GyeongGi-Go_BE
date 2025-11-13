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

        // 1. 거리 계산 및 정렬 로직 (변경 없음)
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
                    String addressToQuery = Optional.ofNullable(row.getSmReAddr())
                            .filter(a -> !a.isEmpty())
                            .orElse(row.getSigunNm());

                    Optional<Place> placeOptional = placeRepository.findByPlaceNameAndAddress(placeName, addressToQuery);

                    if (placeOptional.isPresent()) {
                        // DB의 Place 엔티티와 API의 Row 데이터를 결합하여 HomePlaceDto 생성
                        return new HomePlaceDto(row, placeOptional.get());
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
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