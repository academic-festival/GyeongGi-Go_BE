package academic_festival.gyeonggi_go.Home.Service;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HomeService {

    private final GgApiService ggApiService;

    public HomeService(GgApiService ggApiService) {
        this.ggApiService = ggApiService;
    }

    public List<GgApiResponse.Row> getNearestTourData(double userLat, double userLon) {
        // GgApiService를 통해 모든 관광지 데이터 로드 (중복 제거된 데이터)
        List<GgApiResponse.Row> allTourData = ggApiService.fetchAllTourDataByAllKeys();

        // 관광지 데이터를 사용자 위치 기준으로 거리 계산 및 정렬
        List<GgApiResponse.Row> sortedList = allTourData.stream()
                // 좌표 정보가 유효한 데이터만 필터링합니다.
                .filter(row -> row.getRefineWgs84Lat() != null && row.getRefineWgs84Logt() != null)
                .filter(row -> !row.getRefineWgs84Lat().trim().isEmpty() && !row.getRefineWgs84Logt().trim().isEmpty())
                .map(row -> {
                    try {
                        double dataLat = Double.parseDouble(row.getRefineWgs84Lat());
                        double dataLon = Double.parseDouble(row.getRefineWgs84Logt());
                        
                        return new DistanceWrapper(row, calculateEuclideanDistance(userLat, userLon, dataLat, dataLon));
                    } catch (NumberFormatException e) {
                        // 파싱 오류 발생 시 null 반환하여 필터링
                        return null;
                    }
                })
                .filter(Objects::nonNull) // null 값 제거 (파싱 오류 데이터)
                .sorted(Comparator.comparingDouble(wrapper -> wrapper.distance)) // 거리를 기준으로 오름차순 정렬 (가까운 순)
                .map(wrapper -> wrapper.row) // 정렬된 Row 객체만 추출
                .collect(Collectors.toList());

        System.out.println("\n사용자 위치(" + userLat + ", " + userLon + ") 기반, 거리 순으로 " + sortedList.size() + "개의 명소 정렬 완료.");
        return sortedList;
    }

    private double calculateEuclideanDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
    }

    // 정렬을 위해 Row 객체와 거리를 임시로 묶어주는 내부 클래스
    private static class DistanceWrapper {
        final GgApiResponse.Row row;
        final double distance;

        DistanceWrapper(GgApiResponse.Row row, double distance) {
            this.row = row;
            this.distance = distance;
        }
    }
}