package pl.edu.agh.car_service.Models.Reservation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AddReservationDto {
    private Long idOffer;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
