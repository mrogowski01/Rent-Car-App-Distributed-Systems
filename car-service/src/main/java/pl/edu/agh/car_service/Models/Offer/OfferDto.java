package pl.edu.agh.car_service.Models.Offer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.agh.car_service.Models.Car.CarDto;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class OfferDto {
    private Long id;
    private Long carId;
    private Long userId;
    private Long price;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private CarDto car;
}
