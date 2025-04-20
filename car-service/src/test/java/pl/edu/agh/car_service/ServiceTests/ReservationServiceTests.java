package pl.edu.agh.car_service.ServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import pl.edu.agh.car_service.Configuration.Authorization.JwtUtils;
import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Entities.Offer;
import pl.edu.agh.car_service.Entities.Reservation;
import pl.edu.agh.car_service.Exceptions.InvalidReservationDateException;
import pl.edu.agh.car_service.Helpers.MailHelper;
import pl.edu.agh.car_service.Helpers.OwnerMailProvider;
import pl.edu.agh.car_service.Models.Mail.MailStructure;
import pl.edu.agh.car_service.Models.Reservation.AddReservationDto;
import pl.edu.agh.car_service.Models.Reservation.ReservationDto;
import pl.edu.agh.car_service.Repositories.CarRepository;
import pl.edu.agh.car_service.Repositories.OfferRepository;
import pl.edu.agh.car_service.Repositories.ReservationRepository;
import pl.edu.agh.car_service.Services.ReservationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationServiceTest {
    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private MailHelper mailhelper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private OwnerMailProvider ownermailprovider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createReservation_validReservation_shouldCreateSuccessfully() {
        AddReservationDto reservationDto = new AddReservationDto(1L, LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 10));
        Offer offer = new Offer(1L, 1L, 1L, 100L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null);
        Car car = new Car(1L, "BMW", "X1", 2020, 2.0, "Diesel", "Black", "Manual", 1L);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(reservationRepository.findByIdOffer(1L)).thenReturn(List.of());
        var mockedReservation = new Reservation();
        mockedReservation.setIdReservation(1L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(mockedReservation);
        when(mailhelper.sendMail(any(MailStructure.class))).thenReturn(true);
        when(ownermailprovider.fetchEmailWithBearerToken(any(String.class), any(String.class))).thenReturn("bartosz@babacki.pl");
        when(jwtUtils.getUserId(anyString())).thenReturn(2L);

        Long reservationId = reservationService.createReservation(reservationDto, 2L, "adam@abacki.pl", "token");

        assertNotNull(reservationId);
        assertEquals(1L, reservationId);
    }

    @Test
    void createReservation_offerNotFound_shouldThrowException() {
        AddReservationDto reservationDto = new AddReservationDto(1L, LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 10));

        when(offerRepository.findById(1L)).thenReturn(Optional.empty());
        when(ownermailprovider.fetchEmailWithBearerToken(any(String.class), any(String.class))).thenReturn("bartosz@babacki.pl");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(reservationDto, 2L, "adam@abacki.pl", "token"));

        assertEquals("Offer not found for id: 1", exception.getMessage());
    }

    @Test
    void createReservation_dateOverlap_shouldThrowException() {
        AddReservationDto reservationDto = new AddReservationDto(1L, LocalDate.of(2025, 5, 5), LocalDate.of(2025, 5, 15));
        Offer offer = new Offer(1L, 1L, 1L, 100L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null);
        Reservation existingReservation = new Reservation(1L, 1L, 2L, LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 20), null);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(reservationRepository.findByIdOffer(1L)).thenReturn(List.of(existingReservation));
        when(ownermailprovider.fetchEmailWithBearerToken(any(String.class), any(String.class))).thenReturn("bartosz@babacki.pl");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(reservationDto, 2L, "adam@abacki.pl", "token"));

        assertTrue(exception.getMessage().contains("The selected dates overlap with an existing reservation. \nYour reservation dates: 2025-05-05 - 2025-05-15, Existing reservation dates: 2025-05-10 - 2025-05-20"));
    }

    @Test
    void createReservation_invalidDateRange_shouldThrowException() {
        AddReservationDto reservationDto = new AddReservationDto(1L, LocalDate.of(2025, 5, 15), LocalDate.of(2025, 5, 10));
        Offer offer = new Offer(1L, 1L, 1L, 100L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(ownermailprovider.fetchEmailWithBearerToken(any(String.class), any(String.class))).thenReturn("bartosz@babacki.pl");

        InvalidReservationDateException exception = assertThrows(InvalidReservationDateException.class,
                () -> reservationService.createReservation(reservationDto, 2L, "adam@abacki.pl", "token"));

        assertEquals("dateTo must be after dateFrom", exception.getMessage());
    }

    @Test
    void createReservation_reservationDatesOutsideOfferAvailability_shouldThrowException() {
        AddReservationDto reservationDto = new AddReservationDto(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));
        Offer offer = new Offer(1L, 1L, 1L, 100L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        InvalidReservationDateException exception = assertThrows(InvalidReservationDateException.class,
                () -> reservationService.createReservation(reservationDto, 2L, "adam@abacki.pl", "token"));

        assertEquals("dateTo must be before offer's availableTo: 2025-12-31", exception.getMessage());
    }

    @Test
    void createReservation_dateFromBeforeAvailableFrom_shouldThrowException() {
        AddReservationDto reservationDto = new AddReservationDto(1L, LocalDate.of(2024, 12, 25), LocalDate.of(2025, 1, 5));
        Offer offer = new Offer(1L, 1L, 1L, 100L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        InvalidReservationDateException exception = assertThrows(InvalidReservationDateException.class,
                () -> reservationService.createReservation(reservationDto, 2L, "adam@abacki.pl", "token"));

        assertEquals("dateFrom must be after offer's availableFrom: 2025-01-01", exception.getMessage());
    }

    @Test
    void createReservation_dateToAfterAvailableTo_shouldThrowException() {
        AddReservationDto reservationDto = new AddReservationDto(1L, LocalDate.of(2025, 12, 25), LocalDate.of(2026, 1, 5));
        Offer offer = new Offer(1L, 1L, 1L, 100L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        InvalidReservationDateException exception = assertThrows(InvalidReservationDateException.class,
                () -> reservationService.createReservation(reservationDto, 2L, "adam@abacki.pl", "token"));

        assertEquals("dateTo must be before offer's availableTo: 2025-12-31", exception.getMessage());
    }

    @Test
    void getReservationById_reservationExists_shouldReturnReservationDto() {
        Reservation reservation = new Reservation(1L, 1L, 1L, LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 10), null);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        ReservationDto result = reservationService.getReservationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getIdReservation());
        assertEquals(LocalDate.of(2025, 5, 1), result.getDateFrom());
        assertEquals(LocalDate.of(2025, 5, 10), result.getDateTo());
    }

    @Test
    void getReservationById_reservationNotFound_shouldReturnNull() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        ReservationDto result = reservationService.getReservationById(1L);

        assertNull(result);
    }

    @Test
    void deleteReservation_validId_shouldDeleteSuccessfully() {
        doNothing().when(reservationRepository).deleteById(1L);

        assertDoesNotThrow(() -> reservationService.deleteReservation(1L));

        verify(reservationRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteReservation_invalidId_shouldDoNothing() {
        doNothing().when(reservationRepository).deleteById(1L);

        assertDoesNotThrow(() -> reservationService.deleteReservation(999L));

        verify(reservationRepository, times(1)).deleteById(999L);
    }
}
