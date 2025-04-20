package pl.edu.agh.car_service.Models.Reservation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.agh.car_service.Models.Offer.OfferDto;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReservationDto {
    private Long idReservation;
    private Long idOffer;
    private Long idUser;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private OfferDto offer;
}
