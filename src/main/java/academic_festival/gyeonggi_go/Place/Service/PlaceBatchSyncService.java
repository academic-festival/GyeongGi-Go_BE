package academic_festival.gyeonggi_go.Place.Service;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Service.GgApiService;
import academic_festival.gyeonggi_go.Place.Domain.Place;
import academic_festival.gyeonggi_go.Place.Repository.PlaceRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlaceBatchSyncService {

    private final GgApiService ggApiService;
    private final PlaceRepository placeRepository;

    public PlaceBatchSyncService(GgApiService ggApiService, PlaceRepository placeRepository) {
        this.ggApiService = ggApiService;
        this.placeRepository = placeRepository;
    }

    @Scheduled(cron = "0 0 3 * * *") // cron만 사용
    @Transactional
    public void scheduleDailySync() {
        System.out.println("--- [cron 실행] 매일 새벽 3시 정기 동기화 시작 ---");
        this.syncGyeonggiTourData();
    }

    @Scheduled(initialDelay = 0, fixedDelay = Long.MAX_VALUE) // initialDelay만 사용하고, 이후 반복은 하지 않도록 설정
    @Transactional
    public void scheduleInitialSync() {
        System.out.println("--- [initialDelay 실행] 애플리케이션 시작 후 최초 동기화 시작 ---");
        this.syncGyeonggiTourData();
    }


    @Transactional
    public void syncGyeonggiTourData() {
        System.out.println("=================================================");
        System.out.println("[배치] 경기도 관광지 데이터 동기화 시작 (ETL)");
        long startTime = System.currentTimeMillis();

        // 추출 : GgApiService를 통해 모든 관광지 데이터 로드
        List<GgApiResponse.Row> rawDataList = ggApiService.fetchAllTourDataByAllKeys();

        System.out.println("[ETL-Extract] API에서 로드된 유니크 데이터 총 건수: " + rawDataList.size() + "건");

        // 변환 및 적재 : Row 객체를 Place 엔티티로 변환 후 DB에 저장/업데이트
        int updatedCount = 0;
        int insertedCount = 0;
        int skippedCount = 0;

        for (GgApiResponse.Row row : rawDataList) {
            Optional<Place> transformedPlace = transformToPlace(row);

            if (transformedPlace.isPresent()) {
                Place newPlace = transformedPlace.get();

                // 중복 확인 로직 (PlaceName과 Address 기반)
                Optional<Place> existingPlace = placeRepository.findByPlaceNameAndAddress(newPlace.getPlaceName(), newPlace.getAddress());

                if (existingPlace.isPresent()) {
                    // 데이터 업데이트 (Update)
                    Place placeToUpdate = existingPlace.get();
                    updatePlaceFields(placeToUpdate, newPlace); // 필요한 필드만 업데이트
                    placeRepository.save(placeToUpdate);
                    updatedCount++;
                } else {
                    // 신규 데이터 삽입 (Insert)
                    placeRepository.save(newPlace);
                    insertedCount++;
                }
            } else {
                skippedCount++; // 좌표 정보가 없거나 파싱 오류로 인해 건너뜀
            }
        }

        long endTime = System.currentTimeMillis();

        System.out.println("[ETL-Load] 데이터 동기화 완료!");
        System.out.println(" - 신규 삽입: " + insertedCount + "건");
        System.out.println(" - 기존 업데이트: " + updatedCount + "건");
        System.out.println(" - 데이터 누락/건너뜀: " + skippedCount + "건");
        System.out.println(" - 총 처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("=================================================");
    }

    // GgApiResponse.Row를 Place 엔티티로 변환
    private Optional<Place> transformToPlace(GgApiResponse.Row row) {
        String name = row.getTurSmInfoNmForOutput();
        String address = Optional.ofNullable(row.getSmReAddr()).orElse(""); // ETST, TTST API용 주소 사용
        if (address.isEmpty()) {

        }

        try {
            if (name == null || name.trim().isEmpty() || row.getRefineWgs84Lat() == null || row.getRefineWgs84Logt() == null) {
                return Optional.empty(); // 필수 필드가 없으면 변환하지 않음
            }

            Double lat = Double.parseDouble(row.getRefineWgs84Lat());
            Double lon = Double.parseDouble(row.getRefineWgs84Logt());

            Place place = new Place();
            place.setPlaceName(name);
            place.setAddress(address.isEmpty() ? row.getSigunNm() : address);
            place.setY(lat);
            place.setX(lon);

            // 기타 필드 매핑 (현재 Row DTO에 없는 정보는 일단 null 또는 기본값)
            place.setInquiry(row.getTelNo());

            return Optional.of(place);
        } catch (NumberFormatException e) {
            System.err.println("좌표 파싱 오류: " + row.getRefineWgs84Lat() + ", " + row.getRefineWgs84Logt());
            return Optional.empty();
        }
    }

    // 기존 데이터에 새 데이터의 변경 가능한 필드만 업데이트
    private void updatePlaceFields(Place existing, Place newPlace) {
        existing.setAddress(newPlace.getAddress());
        existing.setX(newPlace.getX());
        existing.setY(newPlace.getY());
        existing.setInquiry(newPlace.getInquiry());
    }
}
