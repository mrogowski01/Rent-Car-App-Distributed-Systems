package pl.edu.agh.car_service.Mappers;

import pl.edu.agh.car_service.Entities.Reservation;
import pl.edu.agh.car_service.Models.Reservation.AddReservationDto;
import pl.edu.agh.car_service.Models.Reservation.ReservationDto;

public class ReservationMapper {
    public static ReservationDto ReservationToReservationDto(Reservation reservation) {
        return new ReservationDto(reservation.getIdReservation(),
                reservation.getIdOffer(),
                reservation.getIdUser(),
                reservation.getDateFrom(),
                reservation.getDateTo(),
                reservation.getOfferDetails() == null ? null : OfferMapper.OfferToOfferDto(reservation.getOfferDetails()));
    }

    public static Reservation ReservationDtoToReservation(ReservationDto reservationDto) {
        return new Reservation(reservationDto.getIdReservation(),
                reservationDto.getIdOffer(),
                reservationDto.getIdUser(),
                reservationDto.getDateFrom(),
                reservationDto.getDateTo(),
                reservationDto.getOffer() == null ? null : OfferMapper.OfferDtoToOffer(reservationDto.getOffer()));
    }

    public static Reservation AddReservationDtoToReservation(AddReservationDto reservationDto, Long userId) {
        return new Reservation(null,
                reservationDto.getIdOffer(),
                userId,
                reservationDto.getDateFrom(),
                reservationDto.getDateTo(),
                null);
    }
}