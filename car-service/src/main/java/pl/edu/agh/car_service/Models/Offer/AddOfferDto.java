package pl.edu.agh.car_service.Models.Offer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AddOfferDto {
    private Long carId;
    private Long price;
    private LocalDate availableFrom;
    private LocalDate availableTo;
}
