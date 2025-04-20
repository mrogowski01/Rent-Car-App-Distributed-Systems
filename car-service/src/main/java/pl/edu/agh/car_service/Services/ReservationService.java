package pl.edu.agh.car_service.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import pl.edu.agh.car_service.Configuration.Authorization.JwtUtils;
import pl.edu.agh.car_service.Configuration.Mail.ReservationConstants;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Exceptions.InvalidReservationDateException;
import pl.edu.agh.car_service.Helpers.MailHelper;
import pl.edu.agh.car_service.Helpers.OwnerMailProvider;
import pl.edu.agh.car_service.Mappers.ReservationMapper;
import pl.edu.agh.car_service.Models.Mail.MailStructure;
import pl.edu.agh.car_service.Models.Reservation.AddReservationDto;
import pl.edu.agh.car_service.Models.Reservation.ReservationDto;
import pl.edu.agh.car_service.Repositories.CarRepository;
import pl.edu.agh.car_service.Repositories.OfferRepository;
import pl.edu.agh.car_service.Repositories.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Entities.Offer;
import pl.edu.agh.car_service.Entities.Reservation;

import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;

import static pl.edu.agh.car_service.Configuration.Mail.ReservationConstants.*;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final OfferRepository offerRepository;
    private final CarRepository carRepository;
    private final MailHelper mailHelper;
    private final OwnerMailProvider ownerMailProvider;
    private final JwtUtils jwtUtils;
    @Value("${mail.notification.user.url}")
    private String URL;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository, OfferRepository offerRepository, CarRepository carRepository, MailHelper mailHelper, OwnerMailProvider ownerMailProvider, JwtUtils jwtUtils) {
        this.reservationRepository = reservationRepository;
        this.offerRepository = offerRepository;
        this.carRepository = carRepository;
        this.mailHelper = mailHelper;
        this.ownerMailProvider = ownerMailProvider;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public Long createReservation(AddReservationDto reservation, Long userId, String userMail, String token) {
        validateReservationDates(reservation);

        Optional<Offer> offerOptional = offerRepository.findById(reservation.getIdOffer());
        if (offerOptional.isEmpty()) {
            throw new RuntimeException("Offer not found for id: " + reservation.getIdOffer());
        }
        Offer offer = offerOptional.get();
        if (offer.getIdUser().equals(userId)) {
            throw new RuntimeException("You can't create reservation for your own offer.");
        }

        List<Reservation> existingReservations = reservationRepository.findByIdOffer(offer.getIdOffer());
        for (Reservation existingReservation : existingReservations) {
            if (isDateOverlap(reservation.getDateFrom(), reservation.getDateTo(), existingReservation.getDateFrom(), existingReservation.getDateTo())) {
                String errorMessage = String.format(
                        "The selected dates overlap with an existing reservation. \nYour reservation dates: %s - %s, Existing reservation dates: %s - %s",
                        reservation.getDateFrom(), reservation.getDateTo(),
                        existingReservation.getDateFrom(), existingReservation.getDateTo()
                );
                throw new RuntimeException(errorMessage);
            }
        }
        sendMail(reservation, token);

        Reservation savedReservation = reservationRepository.save(ReservationMapper.AddReservationDtoToReservation(reservation, userId));
        return savedReservation.getIdReservation();
    }

    public void sendMail(AddReservationDto reservation, String token) {
        Long userId = jwtUtils.getUserId(token);
        String userMail = jwtUtils.getUserMail(token);

        Optional<Offer> offerOptional = offerRepository.findById(reservation.getIdOffer());
        if (offerOptional.isEmpty()) {
            throw new RuntimeException("Offer not found for id: " + reservation.getIdOffer());
        }
        Offer offer = offerOptional.get();
        if (offer.getIdUser().equals(userId)) {
            throw new RuntimeException("You can't create reservation for your own offer.");
        }

        Long ownerId = offer.getIdUser();
//        String ownerEmailUrl = "http://user-service:8082/api/auth/users/" + ownerId + "/email";
//        String ownerEmail = ownerMailProvider.fetchEmailWithBearerToken(ownerEmailUrl, token);

        String ownerEmail = null;
        if (Boolean.parseBoolean(System.getProperty("user-service.enabled", "true"))) {
            String ownerEmailUrl = "http://user-service:8082/api/auth/users/" + ownerId + "/email";
            ownerEmail = ownerMailProvider.fetchEmailWithBearerToken(ownerEmailUrl, token);
        } else {
            ownerEmail = "default-owner@example.com";
        }

        if (ownerEmail == null) {
            throw new RuntimeException("Owner not found for offer id: " + offer.getIdOffer());
        }

        Optional<Car> carOptional = carRepository.findById(offer.getIdCar());
        if (carOptional.isPresent()) {
            offer.setCarDetails(carOptional.get());
        } else {
            throw new RuntimeException("Car not found for id: " + offer.getIdCar());
        }

        var days = Period.between(reservation.getDateFrom(), reservation.getDateTo()).getDays();

        String ownerMailContent = String.format(ownerReservationMailContentCreate,
                userMail,
                offer.getCarDetails().getBrand(),
                offer.getCarDetails().getModel(),
                offer.getIdOffer(),
                offer.getPrice(),
                offer.getAvailableFrom(),
                offer.getAvailableTo(),
                reservation.getDateFrom(),
                reservation.getDateTo(),
                (days + 1) * offer.getPrice()
        );

        mailHelper.sendMail(new MailStructure(userMail, ReservationConstants.reservationMailTitleCreate, String.format(ReservationConstants.reservationMailContentCreate, offer.getCarDetails().getBrand(), offer.getCarDetails().getModel(), reservation.getDateFrom(), reservation.getDateTo(), (days + 1) * offer.getPrice())));
        mailHelper.sendMail(new MailStructure(ownerEmail, ReservationConstants.reservationMailTitleCreate, ownerMailContent));
    }

    public void sendMail(Long idReservation, String token) {
        Long userId = jwtUtils.getUserId(token);
        String userMail = jwtUtils.getUserMail(token);

        ReservationDto reservation = getReservationById(idReservation);

        Optional<Offer> offerOptional = offerRepository.findById(reservation.getIdOffer());
        if (offerOptional.isEmpty()) {
            throw new RuntimeException("Offer not found for id: " + reservation.getIdOffer());
        }
        Offer offer = offerOptional.get();

        if (offer.getIdUser().equals(userId)) {
            throw new RuntimeException("You can't create a reservation for your own offer.");
        }

        Long ownerId = offer.getIdUser();
        String ownerEmail = null;

        if (Boolean.parseBoolean(System.getProperty("user-service.enabled", "true"))) {
            String ownerEmailUrl = "http://user-service:8082/api/auth/users/" + ownerId + "/email";
            ownerEmail = ownerMailProvider.fetchEmailWithBearerToken(ownerEmailUrl, token);
        } else {
            ownerEmail = "default-owner@example.com";
        }

        if (ownerEmail == null) {
            throw new RuntimeException("Owner not found for offer id: " + offer.getIdOffer());
        }

        Optional<Car> carOptional = carRepository.findById(offer.getIdCar());
        if (carOptional.isPresent()) {
            offer.setCarDetails(carOptional.get());
        } else {
            throw new RuntimeException("Car not found for id: " + offer.getIdCar());
        }

        var days = Period.between(reservation.getDateFrom(), reservation.getDateTo()).getDays();

        String ownerMailContent = String.format(ownerReservationMailContentDelete,
                userMail,
                offer.getCarDetails().getBrand(),
                offer.getCarDetails().getModel(),
                offer.getIdOffer(),
                offer.getPrice(),
                offer.getAvailableFrom(),
                offer.getAvailableTo(),
                reservation.getDateFrom(),
                reservation.getDateTo(),
                (days + 1) * offer.getPrice()
        );

        mailHelper.sendMail(new MailStructure(userMail, ReservationConstants.reservationMailTitleDelete,
                String.format(ReservationConstants.reservationMailContentDelete,  offer.getCarDetails().getBrand(), offer.getCarDetails().getModel(), reservation.getDateFrom(),
                        reservation.getDateTo(), (days + 1) * offer.getPrice())));

        mailHelper.sendMail(new MailStructure(ownerEmail, ReservationConstants.reservationMailTitleDelete, ownerMailContent));
    }

    public void sendMail(Long idReservation, String token, LocalDate currDateFrom, LocalDate currDateTo, LocalDate newDateFrom, LocalDate newDateTo) {
        Long userId = jwtUtils.getUserId(token);
        String userMail = jwtUtils.getUserMail(token);

        ReservationDto reservation = getReservationById(idReservation);

        Optional<Offer> offerOptional = offerRepository.findById(reservation.getIdOffer());
        if (offerOptional.isEmpty()) {
            throw new RuntimeException("Offer not found for id: " + reservation.getIdOffer());
        }
        Offer offer = offerOptional.get();

        if (offer.getIdUser().equals(userId)) {
            throw new RuntimeException("You can't create a reservation for your own offer.");
        }

        Long ownerId = offer.getIdUser();
        String ownerEmail = null;

        if (Boolean.parseBoolean(System.getProperty("user-service.enabled", "true"))) {
            String ownerEmailUrl = "http://user-service:8082/api/auth/users/" + ownerId + "/email";
            ownerEmail = ownerMailProvider.fetchEmailWithBearerToken(ownerEmailUrl, token);
        } else {
            ownerEmail = "default-owner@example.com";
        }

        if (ownerEmail == null) {
            throw new RuntimeException("Owner not found for offer id: " + offer.getIdOffer());
        }

        Optional<Car> carOptional = carRepository.findById(offer.getIdCar());
        if (carOptional.isPresent()) {
            offer.setCarDetails(carOptional.get());
        } else {
            throw new RuntimeException("Car not found for id: " + offer.getIdCar());
        }

        var days = Period.between(reservation.getDateFrom(), reservation.getDateTo()).getDays();

        String ownerMailContent = String.format(ownerReservationMailContentUpdate,
                userMail,
                offer.getCarDetails().getBrand(),
                offer.getCarDetails().getModel(),
                offer.getIdOffer(),
                offer.getPrice(),
                offer.getAvailableFrom(),
                offer.getAvailableTo(),
                currDateFrom,
                currDateTo,
                newDateFrom,
                newDateTo,
                (days + 1) * offer.getPrice()
        );

        mailHelper.sendMail(new MailStructure(userMail, ReservationConstants.reservationMailTitleUpdate,
                String.format(ReservationConstants.reservationMailContentUpdate,  offer.getCarDetails().getBrand(), offer.getCarDetails().getModel(), currDateFrom, currDateTo, newDateFrom, newDateTo, (days + 1) * offer.getPrice())));

        mailHelper.sendMail(new MailStructure(ownerEmail, ReservationConstants.reservationMailTitleUpdate, ownerMailContent));
    }


    private boolean isDateOverlap(LocalDate startDate1, LocalDate endDate1, LocalDate startDate2, LocalDate endDate2) {
        return !(endDate1.isBefore(startDate2) || startDate1.isAfter(endDate2));
    }

    private void validateReservationDates(AddReservationDto reservation) {
        Optional<Offer> offer = offerRepository.findById(reservation.getIdOffer());
        if (offer.isEmpty()) {
            throw new RuntimeException("Offer not found for id: " + reservation.getIdOffer());
        }

        if (reservation.getDateTo().isBefore(reservation.getDateFrom())) {
            throw new InvalidReservationDateException("dateTo must be after dateFrom");
        }

        if (reservation.getDateTo().isAfter(offer.get().getAvailableTo()) &&
                reservation.getDateFrom().isBefore(offer.get().getAvailableFrom())) {
            throw new InvalidReservationDateException("Date of reservation must be in range of car availability");
        }

        if (reservation.getDateFrom().isBefore(offer.get().getAvailableFrom())) {
            throw new InvalidReservationDateException("dateFrom must be after offer's availableFrom: " + offer.get().getAvailableFrom());
        }

        if (reservation.getDateTo().isAfter(offer.get().getAvailableTo())) {
            throw new InvalidReservationDateException("dateTo must be before offer's availableTo: " + offer.get().getAvailableTo());
        }
    }

    public void updateReservation(Long idReservation, AddReservationDto reservation, Long userId, String role, String token) {
        Optional<Reservation> existingReservationOptional = reservationRepository.findById(idReservation);
        if (existingReservationOptional.isEmpty()) {
            throw new RuntimeException("Reservation not found.");
        }

        Reservation existingReservation = existingReservationOptional.get();

        if (!existingReservation.getIdUser().equals(userId) && !RoleType.ROLE_ADMIN.toString().equals(role)) {
            throw new RuntimeException("You do not have permission to edit this reservation.");
        }

        Optional<Offer> newOfferOptional = offerRepository.findById(reservation.getIdOffer());
        if (newOfferOptional.isEmpty()) {
            throw new RuntimeException("Wrong ID provided for that reservation.");
        }
        Offer newOffer = newOfferOptional.get();
        if (!newOffer.getIdOffer().equals(existingReservation.getIdOffer())) {
            throw new RuntimeException("The offer ID does not match the current reservation's offer.");
        }

        if (reservation.getDateTo().isAfter(newOffer.getAvailableTo()) &&
                reservation.getDateFrom().isBefore(newOffer.getAvailableFrom())) {
            throw new InvalidReservationDateException("Date of reservation must be in range of car availability");
        }

        Optional<Offer> availableOffers = offerRepository.findByIdOffer(existingReservation.getIdOffer());
        if (availableOffers.isEmpty()) {
            throw new RuntimeException("No available offers found for this reservation.");
        }
        LocalDate newDateFrom = reservation.getDateFrom();
        LocalDate newDateTo = reservation.getDateTo();
        if (newDateTo.isBefore(newDateFrom)) {
            throw new InvalidReservationDateException("New dateTo must be after dateFrom!");
        }

        boolean isValidPeriod = availableOffers.stream().anyMatch(offer -> newDateFrom.compareTo(offer.getAvailableFrom()) >= 0 && newDateTo.compareTo(offer.getAvailableTo()) <= 0);

        if (!isValidPeriod) {
            throw new InvalidReservationDateException("The reservation dates do not fit within any available offer period.");
        }

        List<Reservation> conflictingReservations = reservationRepository.findByIdOffer(existingReservation.getIdOffer()).stream()
                .filter(existingRes -> !existingRes.getIdReservation().equals(idReservation))
                .filter(existingRes ->
                        isDateOverlap(reservation.getDateFrom(), reservation.getDateTo(), existingRes.getDateFrom(), existingRes.getDateTo())).toList();

        if (!conflictingReservations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("The selected dates overlap with an existing reservation.");
            for (Reservation conflictingReservation : conflictingReservations) {
                errorMessage.append(String.format(" Your reservation dates: %s - %s, Conflict reservation dates: %s - %s",
                        reservation.getDateFrom(), reservation.getDateTo(), conflictingReservation.getDateFrom(), conflictingReservation.getDateTo()));
            }
            throw new RuntimeException(errorMessage.toString());
        }
        LocalDate currDateFrom = existingReservation.getDateFrom();
        LocalDate currDateTo = existingReservation.getDateTo();

        existingReservation.setDateFrom(reservation.getDateFrom());
        existingReservation.setDateTo(reservation.getDateTo());
        sendMail(idReservation, token, currDateFrom, currDateTo, newDateFrom, newDateTo);
        reservationRepository.save(existingReservation);
    }

    public ReservationDto getReservationById(Long idReservation) {
        var reservation =  reservationRepository.findById(idReservation);
        return reservation.map(ReservationMapper::ReservationToReservationDto).orElse(null);
    }

    public Optional<Offer> getOfferByIdOffer(Long idOffer) {
        return offerRepository.findById(idOffer);
    }

    public void deleteReservation(Long idReservation) {
        reservationRepository.deleteById(idReservation);
    }

    public List<ReservationDto> getReservationsByUserId(Long idUser) {
        var reservations = reservationRepository.findByIdUser(idUser);
        if (reservations == null)
            return null;
        return reservations.stream().map(ReservationMapper::ReservationToReservationDto).toList();
    }

    public List<ReservationDto> getReservationsWithOffersByUserId(Long userId) {
        return reservationRepository.findByIdUser(userId).stream()
                .map(reservation -> {
                    Offer offer = offerRepository.findByIdOffer(reservation.getIdOffer()).orElse(null);
                    if (offer != null) {
                        Car car = carRepository.findById(offer.getCarId()).orElse(null);
                        offer.setCarDetails(car);
                    }
                    reservation.setOfferDetails(offer);
                    return ReservationMapper.ReservationToReservationDto(reservation);
                })
                .collect(Collectors.toList());
    }

    public List<ReservationDto> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(reservation -> {
                    Offer offer = offerRepository.findByIdOffer(reservation.getIdOffer()).orElse(null);
                    if (offer != null) {
                        Car car = carRepository.findById(offer.getCarId()).orElse(null);
                        offer.setCarDetails(car);
                    }
                    reservation.setOfferDetails(offer);
                    return ReservationMapper.ReservationToReservationDto(reservation);
                })
                .collect(Collectors.toList());
    }

    public void deleteReservationsByCarId(Long carId, Long userId, String role) {
        List<Offer> offers = offerRepository.findByCarId(carId);
        for (Offer offer : offers) {
            List <Reservation> reservations = reservationRepository.findByIdOffer(offer.getIdOffer());
            for (Reservation reservation : reservations)
                if (!RoleType.ROLE_ADMIN.toString().equals(role))
                    throw new RuntimeException("The user has no access to remove the reservation.");

            reservationRepository.deleteAll(reservations);
        }
    }

    public void deleteReservationsByOfferId(Long offerId, Long userId, String role) {
        List<Reservation> reservations = reservationRepository.findByIdOffer(offerId);
            for (Reservation reservation : reservations)
                if (!RoleType.ROLE_ADMIN.toString().equals(role))
                    throw new RuntimeException("The reservation does not belong to the user.");

        reservationRepository.deleteAll(reservations);
    }

    @Transactional
    public void deleteReservationAndUpdateOffer(Long idReservation, Long userId, String role, String token) {
        Optional<Reservation> reservationOptional = reservationRepository.findById(idReservation);
        if (reservationOptional.isEmpty()) throw new RuntimeException("Reservation not found for id: " + idReservation);

        Reservation reservation = reservationOptional.get();
        if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !reservation.getIdUser().equals(userId))
            throw new RuntimeException("The reservation does not belong to the user.");

        Optional<Offer> offerOptional = offerRepository.findById(reservation.getIdOffer());
        if (offerOptional.isEmpty()) {
            throw new RuntimeException("Offer not found for id: " + reservation.getIdOffer());
        }

        Offer offer = offerOptional.get();
        offerRepository.save(offer);
        sendMail(idReservation, token);

        reservationRepository.deleteById(idReservation);
    }
}