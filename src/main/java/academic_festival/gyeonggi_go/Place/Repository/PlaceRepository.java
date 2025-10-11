package academic_festival.gyeonggi_go.Place.Repository;

import academic_festival.gyeonggi_go.Place.Domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByPlaceNameAndAddress(String placeName, String address);
}
