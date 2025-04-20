package pl.edu.agh.car_service.ServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Entities.Offer;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Models.Offer.AddOfferDto;
import pl.edu.agh.car_service.Models.Offer.OfferDto;
import pl.edu.agh.car_service.Repositories.CarRepository;
import pl.edu.agh.car_service.Repositories.OfferRepository;
import pl.edu.agh.car_service.Services.OfferService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OfferServiceTest {

    @InjectMocks
    private OfferService offerService;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Test for createOffer
    @Test
    void createOffer_validOffer_createsSuccessfully() {
        AddOfferDto offerDto = new AddOfferDto(1L, 100L, LocalDate.now(), LocalDate.now().plusDays(5));
        Car car = new Car();
        car.setIdUser(1L);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(offerRepository.findByCarId(1L)).thenReturn(List.of());
        var mockedOffer = new Offer();
        mockedOffer.setIdOffer(1L);
        when(offerRepository.save(any(Offer.class))).thenReturn(mockedOffer);

        Long createdOfferId = offerService.createOffer(offerDto, 1L);

        assertNotNull(createdOfferId);
        verify(offerRepository, times(1)).save(any(Offer.class));
    }

    @Test
    void createOffer_conflictingDates_throwsException() {
        AddOfferDto offerDto = new AddOfferDto(1L, 100L, LocalDate.now(), LocalDate.now().plusDays(5));
        Car car = new Car();
        car.setIdUser(1L);

        Offer existingOffer = new Offer();
        existingOffer.setAvailableFrom(LocalDate.now().minusDays(1));
        existingOffer.setAvailableTo(LocalDate.now().plusDays(2));

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(offerRepository.findByCarId(1L)).thenReturn(List.of(existingOffer));

        assertThrows(RuntimeException.class, () -> offerService.createOffer(offerDto, 1L));
    }

    // Test for getOffersByCar
    @Test
    void getOffersByCar_existingCarId_returnsOffers() {
        Offer offer = new Offer();
        offer.setIdOffer(1L);

        when(offerRepository.findByCarId(1L)).thenReturn(List.of(offer));

        var result = offerService.getOffersByCar(1L);

        assertEquals(1, result.size());
        verify(offerRepository, times(1)).findByCarId(1L);
    }

    // Test for updateOffer
    @Test
    void updateOffer_validDetails_updatesSuccessfully() {
        Offer offer = new Offer();
        offer.setIdOffer(1L);
        offer.setCarId(1L);
        offer.setIdUser(1L);

        OfferDto updatedOffer = new OfferDto(1L, 1L, 1L, 150L, LocalDate.now(), LocalDate.now().plusDays(5), null);

        when(offerRepository.findByIdOffer(1L)).thenReturn(Optional.of(offer));
        when(carRepository.findById(1L)).thenReturn(Optional.of(new Car()));
        when(offerRepository.findByCarId(1L)).thenReturn(List.of());

        offerService.updateOffer(1L, updatedOffer, 1L, RoleType.ROLE_USER.toString());

        verify(offerRepository, times(1)).save(any(Offer.class));
    }

    @Test
    void updateOffer_invalidUser_throwsException() {
        Offer offer = new Offer();
        offer.setIdOffer(1L);
        offer.setCarId(1L);
        offer.setIdUser(2L); // Different user ID

        OfferDto updatedOffer = new OfferDto(1L, 1L, 1L, 150L, LocalDate.now(), LocalDate.now().plusDays(5), null);

        when(offerRepository.findByIdOffer(1L)).thenReturn(Optional.of(offer));

        assertThrows(RuntimeException.class, () -> offerService.updateOffer(1L, updatedOffer, 1L, RoleType.ROLE_USER.toString()));
    }

    // Test for deleteOffer
    @Test
    void deleteOffer_validOffer_deletesSuccessfully() {
        Offer offer = new Offer();
        offer.setIdOffer(1L);
        offer.setIdUser(1L);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        boolean result = offerService.deleteOffer(1L, 1L, RoleType.ROLE_USER.toString());

        assertTrue(result);
        verify(offerRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteOffer_invalidUser_throwsException() {
        Offer offer = new Offer();
        offer.setIdOffer(1L);
        offer.setIdUser(2L); // Different user ID

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(RuntimeException.class, () -> offerService.deleteOffer(1L, 1L, RoleType.ROLE_USER.toString()));
    }
}