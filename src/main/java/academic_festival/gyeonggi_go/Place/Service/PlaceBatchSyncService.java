package academic_festival.gyeonggi_go.Place.Service;

import academic_festival.gyeonggi_go.Home.Dto.GgApiResponse;
import academic_festival.gyeonggi_go.Home.Service.GgApiService;
import academic_festival.gyeonggi_go.Home.Service.ManualPlaceData;
import academic_festival.gyeonggi_go.Place.Domain.Place;
import academic_festival.gyeonggi_go.Place.Repository.PlaceRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PlaceBatchSyncService {

    private final GgApiService ggApiService;
    private final PlaceRepository placeRepository;

    public PlaceBatchSyncService(GgApiService ggApiService, PlaceRepository placeRepository) {
        this.ggApiService = ggApiService;
        this.placeRepository = placeRepository;
    }

    // 매일 새벽 3시 정기 동기화 (API -> DB 저장 -> 수동 업데이트)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void scheduleDailySync() {
        System.out.println("--- [cron 실행] 매일 새벽 3시 정기 동기화 시작 ---");
        this.executeFullSync();
    }

    // 애플리케이션 시작 후 최초 동기화
    @Scheduled(initialDelay = 0, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void scheduleInitialSync() {
        System.out.println("--- [initialDelay 실행] 애플리케이션 시작 후 최초 동기화 시작 ---");
        this.executeFullSync();
    }

    // 전체 동기화 실행 (순서: API 데이터 동기화 후 수동 데이터 업데이트)
    public void executeFullSync() {
        // 1. 경기도 API 데이터 동기화 및 DB 저장/업데이트
        syncGyeonggiTourData();

        // 2. 수동으로 제공된 상세 데이터로 DB 업데이트
        applyManualUpdates();
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

                // 중복 확인 로직 (PlaceName과 X, Y 좌표 기반)
                Optional<Place> existingPlace = placeRepository.findByPlaceNameAndXAndY(
                        newPlace.getPlaceName(),
                        newPlace.getX(),
                        newPlace.getY()
                );

                if (existingPlace.isPresent()) {
                    // 데이터 업데이트 (Update)
                    Place placeToUpdate = existingPlace.get();
                    updatePlaceFields(placeToUpdate, newPlace); // API에서 제공하는 필드만 업데이트
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

        System.out.println("[ETL-Load] API 데이터 동기화 완료!");
        System.out.println(" - 신규 삽입: " + insertedCount + "건");
        System.out.println(" - 기존 업데이트: " + updatedCount + "건");
        System.out.println(" - 데이터 누락/건너뜀: " + skippedCount + "건");
        System.out.println(" - 총 처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("=================================================");
    }

    // 수동 데이터 업데이트 로직 (placeName 기준으로 매칭)
    @Transactional
    public void applyManualUpdates() {
        System.out.println("=================================================");
        System.out.println("[배치] ManualPlaceData 수동 업데이트 시작 (기준: placeName)");
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> manualUpdates = ManualPlaceData.getManualUpdates();
        int updatedCount = 0;
        int skippedCount = 0;

        for (Map<String, Object> update : manualUpdates) {
            String placeName = (String) update.get("placeName");
            Optional<Place> existingPlaceOptional = placeRepository.findByPlaceName(placeName); // findByPlaceNameAndAddress -> findByPlaceName 변경

            if (existingPlaceOptional.isPresent()) {
                Place placeToUpdate = existingPlaceOptional.get();

                // 수동으로 제공된 필드 업데이트
                placeToUpdate.setLocationExplain((String) update.get("location_explain"));
                placeToUpdate.setPrice((String) update.get("price"));

                // ManualPlaceData에 있는 주소로 업데이트
                placeToUpdate.setAddress((String) update.get("address"));

                if (update.get("inquiry") != null) {
                    placeToUpdate.setInquiry((String) update.get("inquiry"));
                }
                if (update.get("operating_hours") != null) {
                    placeToUpdate.setOperatingHours((String) update.get("operating_hours"));
                }

                placeRepository.save(placeToUpdate);
                updatedCount++;
            } else {
                // 매칭 실패 시 출력 메시지도 단순화
                System.err.println("[수동 업데이트 오류] DB에서 일치하는 명소를 찾을 수 없어 건너뜀: " + placeName);
                skippedCount++;
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[배치] ManualPlaceData 수동 업데이트 완료!");
        System.out.println(" - 업데이트 완료: " + updatedCount + "건");
        System.out.println(" - 건너뜀 (미매칭): " + skippedCount + "건");
        System.out.println(" - 총 처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("=================================================");
    }

    // GgApiResponse.Row를 Place 엔티티로 변환 (기존 코드 유지)
    private Optional<Place> transformToPlace(GgApiResponse.Row row) {
        String name = row.getTurSmInfoNmForOutput();
        String address = Optional.ofNullable(row.getSmReAddr()).orElse(""); // ETST, TTST API용 주소 사용

        String finalAddress;
        if (address.isEmpty() || address.trim().isEmpty()) {
            // 상세 주소가 없으면 시군명(예: "수원시") 사용
            finalAddress = row.getSigunNm();
        } else {
            finalAddress = address;
        }

        try {
            if (name == null || name.trim().isEmpty() || row.getRefineWgs84Lat() == null || row.getRefineWgs84Logt() == null) {
                return Optional.empty(); // 필수 필드가 없으면 변환하지 않음
            }

            Double lat = Double.parseDouble(row.getRefineWgs84Lat());
            Double lon = Double.parseDouble(row.getRefineWgs84Logt());

            Place place = new Place();
            place.setPlaceName(name);
            place.setAddress(finalAddress);
            place.setY(lat); // 위도 (Latitude)
            place.setX(lon); // 경도 (Longitude)

            // 기타 필드 매핑
            place.setInquiry(row.getTelNo());

            return Optional.of(place);
        } catch (NumberFormatException e) {
            System.err.println("좌표 파싱 오류: " + row.getRefineWgs84Lat() + ", " + row.getRefineWgs84Logt());
            return Optional.empty();
        }
    }

    // 기존 API 데이터 업데이트 로직 (API에서 제공하는 필드만 업데이트)
    private void updatePlaceFields(Place existing, Place newPlace) {
        existing.setX(newPlace.getX());
        existing.setY(newPlace.getY());
        existing.setInquiry(newPlace.getInquiry());
    }
}