package pl.edu.agh.car_service.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.edu.agh.car_service.Entities.Offer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByCarId(Long carId);
    List<Offer> findByIdUser(Long idUser);
    Optional<Offer> findByIdOffer(Long idOffer);

    @Query("SELECT o FROM Offer o WHERE o.availableFrom >= :currentDate")
    List<Offer> findAllAvailableFromNow(LocalDate currentDate);
}