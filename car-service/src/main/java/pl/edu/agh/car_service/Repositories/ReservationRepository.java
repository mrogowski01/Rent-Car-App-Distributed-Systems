package pl.edu.agh.car_service.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.car_service.Entities.Reservation;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByIdUser(Long idUser);
    List<Reservation> findByIdOffer(Long idOffer);
    List<Reservation> findByIdOfferIn(List<Long> offerIds);
}