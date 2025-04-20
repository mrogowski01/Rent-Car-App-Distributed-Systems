package pl.edu.agh.car_service.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReservation;

    @Column(name = "id_offer", nullable = false)
    private Long idOffer;

    @Column(name = "id_user")
    private Long idUser;

    private LocalDate dateFrom;
    private LocalDate dateTo;

    @Transient
    private Offer offerDetails;

    public void setOfferDetails(Offer offerDetails) {
        this.offerDetails = offerDetails;
    }
}