package pl.edu.agh.car_service.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOffer;

    @Column(name = "id_car", nullable = false)
    private Long carId;

    @Column(name = "id_user")
    private Long idUser;

    private Long price;

    private LocalDate availableFrom;

    private LocalDate availableTo;

    @Setter
    @Transient
    private Car carDetails;

    public Long getIdCar() {
        return carId;
    }

}