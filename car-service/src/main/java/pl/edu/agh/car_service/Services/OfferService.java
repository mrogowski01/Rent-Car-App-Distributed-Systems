package pl.edu.agh.car_service.Services;

import pl.edu.agh.car_service.Entities.Reservation;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Mappers.CarMapper;
import pl.edu.agh.car_service.Mappers.OfferMapper;
import pl.edu.agh.car_service.Models.Car.CarDto;
import pl.edu.agh.car_service.Models.Offer.AddOfferDto;
import pl.edu.agh.car_service.Models.Offer.OfferDto;
import pl.edu.agh.car_service.Repositories.CarRepository;
import pl.edu.agh.car_service.Repositories.OfferRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Entities.Offer;
import pl.edu.agh.car_service.Repositories.ReservationRepository;

import java.util.stream.Collectors;

@Service
public class OfferService {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    public Long createOffer(AddOfferDto offer, Long userId) {
        if (offer.getAvailableFrom().isAfter(offer.getAvailableTo())) {
            throw new RuntimeException("Offer start date must be before end date.");
        }

        Optional<Car> optionalCar = carRepository.findById(offer.getCarId());
        if (optionalCar.isEmpty()) {
            throw new RuntimeException("Car does not exist.");
        }

        Car car = optionalCar.get();
        if (!car.getIdUser().equals(userId)) {
            throw new RuntimeException("You can only create offers for your own cars.");
        }

        List<Offer> existingOffers = offerRepository.findByCarId(offer.getCarId());
        for (Offer existingOffer : existingOffers) {
            if (!(offer.getAvailableTo().isBefore(existingOffer.getAvailableFrom()) ||
                    offer.getAvailableFrom().isAfter(existingOffer.getAvailableTo()))) {
                throw new RuntimeException("There is already an offer for this car with the specified dates.");
            }
        }

        Offer savedOffer = offerRepository.save(OfferMapper.AddOfferDtoToOffer(offer, userId));
        return savedOffer.getIdOffer();
    }

    public List<OfferDto> getOffersByCar(Long carId) {
        var offers = offerRepository.findByCarId(carId);
        return offers.stream().map(OfferMapper::OfferToOfferDto).toList();
    }

    public OfferDto findByIdOffer(Long IdOffer) {
        var offer = offerRepository.findByIdOffer(IdOffer);
        return offer.map(OfferMapper::OfferToOfferDto).orElse(null);
    }

    public List<OfferDto> getAllOffers() {
        return offerRepository.findAll().stream().map(OfferMapper::OfferToOfferDto).toList();
    }

    public List<OfferDto> getOffersByUserId(Long userId) {
        return offerRepository.findByIdUser(userId).stream().map(OfferMapper::OfferToOfferDto).toList();
    }

    public List<CarDto> getCarsForOffersByUserId(Long userId) {
        List<Offer> offers = offerRepository.findByIdUser(userId);
        return offers.stream()
                .map(offer -> carRepository.findById(offer.getCarId()).orElse(null)).filter(Objects::nonNull).map(CarMapper::CarToCarDto).toList().reversed();
    }

    public List<OfferDto> getOffersWithCarsByUserId(Long userId) {
        return offerRepository.findByIdUser(userId).stream()
                .map(offer -> {
                    Car car = carRepository.findById(offer.getCarId()).orElse(null);
                    var offerDto = OfferMapper.OfferToOfferDto(offer);
                    if (car != null)
                        offerDto.setCar(CarMapper.CarToCarDto(car));
                    return offerDto;
                })
                .collect(Collectors.toList());
    }

    public List<OfferDto> getAllOffersWithCars() {
        return offerRepository.findAll().stream()
                .map(offer -> {
                    Car car = carRepository.findById(offer.getCarId()).orElse(null);
                    offer.setCarDetails(car);
                    return OfferMapper.OfferToOfferDto(offer);
                })
                .collect(Collectors.toList());
    }

    private static final Logger logger = LoggerFactory.getLogger(OfferService.class);

    public void updateOffer(Long id, OfferDto offerDetails, Long userId, String role) {
        Optional<Offer> optionalOffer = offerRepository.findByIdOffer(id);
        if (optionalOffer.isPresent()) {
            Offer offer = optionalOffer.get();

            if (!offer.getCarId().equals(offerDetails.getCarId())) {
                throw new RuntimeException("The car ID does not match the car ID in current offer.");
            }

            if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !offer.getIdUser().equals(userId)) {
                throw new RuntimeException("The offer does not belong to the user.");
            }

            if (offerDetails.getAvailableFrom().isAfter(offerDetails.getAvailableTo())) {
                throw new RuntimeException("Offer start date must be before end date.");
            }

            Car car = carRepository.findById(offerDetails.getCarId())
                    .orElseThrow(() -> new RuntimeException("Car does not exist."));

            List<Offer> conflictingOffers = offerRepository.findByCarId(offerDetails.getCarId()).stream()
                    .filter(existingOffer -> !existingOffer.getIdOffer().equals(id))
                    .filter(existingOffer ->
                            !(offerDetails.getAvailableTo().isBefore(existingOffer.getAvailableFrom()) ||
                                    offerDetails.getAvailableFrom().isAfter(existingOffer.getAvailableTo()))
                    )
                    .toList();

            if (!conflictingOffers.isEmpty()) {
                throw new RuntimeException("The provided dates overlap with other offers for the same car.");
            }

            offer.setPrice(offerDetails.getPrice());
            offer.setAvailableFrom(offerDetails.getAvailableFrom());
            offer.setAvailableTo(offerDetails.getAvailableTo());
            offerRepository.save(offer);
        } else {
            throw new RuntimeException("Offer not found.");
        }
    }

    public List<OfferDto> getAdjustedOffers() {
        LocalDate now = LocalDate.now();
        List<Offer> offers = offerRepository.findAllAvailableFromNow(now);

        List<Long> offerIds = offers.stream().map(Offer::getIdOffer).collect(Collectors.toList());
        List<Reservation> reservations = reservationRepository.findByIdOfferIn(offerIds);

        List<OfferDto> adjustedOffers = new ArrayList<>();

        for (Offer offer : offers) {
            LocalDate availableFrom = offer.getAvailableFrom();
            LocalDate availableTo = offer.getAvailableTo();

            List<Reservation> relevantReservations = reservations.stream()
                    .filter(res -> res.getIdOffer().equals(offer.getIdOffer()))
                    .toList();

            for (Reservation reservation : relevantReservations) {
                LocalDate reservationStart = reservation.getDateFrom();
                LocalDate reservationEnd = reservation.getDateTo();

                if (reservationStart.isAfter(availableFrom) && reservationStart.isBefore(availableTo)) {
                    Offer offerBeforeReservation = new Offer(
                            offer.getIdOffer(),
                            offer.getCarId(),
                            offer.getIdUser(),
                            offer.getPrice(),
                            availableFrom,
                            reservationStart.minusDays(1),
                            carRepository.findById(offer.getCarId()).orElse(null)
                    );
                    adjustedOffers.add(OfferMapper.OfferToOfferDto(offerBeforeReservation));

                    availableFrom = reservationEnd.plusDays(1);
                }
            }

            if (!availableFrom.isAfter(availableTo)) {
                Offer adjustedOffer = new Offer(
                        offer.getIdOffer(),
                        offer.getCarId(),
                        offer.getIdUser(),
                        offer.getPrice(),
                        availableFrom,
                        availableTo,
                        carRepository.findById(offer.getCarId()).orElse(null)
                );
                adjustedOffers.add(OfferMapper.OfferToOfferDto(adjustedOffer));
            }
        }
        return adjustedOffers;
    }


    public boolean deleteOffer(Long id, Long userId, String role) {
        Optional<Offer> optionalOffer = offerRepository.findById(id);
        if (optionalOffer.isPresent()) {
            if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !optionalOffer.get().getIdUser().equals(userId))
                throw new RuntimeException("The offer does not belong to the user.");

            offerRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public void deleteOffersByCarId(Long carId, Long userId, String role) {
        List<Offer> offers = offerRepository.findByCarId(carId);
        for (Offer offer : offers) {
            if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !offer.getIdUser().equals(userId))
                throw new RuntimeException("The offer does not belong to the user.");
            offerRepository.delete(offer);
        }
    }
}