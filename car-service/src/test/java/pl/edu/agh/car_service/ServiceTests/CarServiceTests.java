package pl.edu.agh.car_service.ServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Models.Car.AddCarDto;
import pl.edu.agh.car_service.Models.Car.CarDto;
import pl.edu.agh.car_service.Repositories.CarRepository;
import pl.edu.agh.car_service.Services.CarService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarServiceTest {

    @InjectMocks
    private CarService carService;

    @Mock
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Test for findAll
    @Test
    void findAll_returnsCars() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("Toyota");

        when(carRepository.findAll()).thenReturn(List.of(car));

        List<CarDto> result = carService.findAll();

        assertEquals(1, result.size());
        assertEquals("Toyota", result.getFirst().getBrand());
        verify(carRepository, times(1)).findAll();
    }

    // Test for findById
    @Test
    void findById_existingCarId_returnsCarDto() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("Toyota");

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        CarDto result = carService.findById(1L);

        assertNotNull(result);
        assertEquals("Toyota", result.getBrand());
        verify(carRepository, times(1)).findById(1L);
    }

    @Test
    void findById_nonExistingCarId_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> carService.findById(1L));
    }

    // Test for save
    @Test
    void save_validCarDto_savesSuccessfully() {
        AddCarDto carDto = new AddCarDto("Toyota", "Corolla", 2020, 2.0, "Petrol", "Red", "Manual");
        Long userId = 1L;

        var mockedCar = new Car();
        mockedCar.setId(1L);
        when(carRepository.save(any(Car.class))).thenReturn(mockedCar);

        Long carId = carService.save(carDto, userId);

        assertNotNull(carId);
        verify(carRepository, times(1)).save(any(Car.class));
    }

    // Test for deleteById
    @Test
    void deleteById_validCar_deletesSuccessfully() {
        Car car = new Car();
        car.setId(1L);
        car.setIdUser(1L);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        carService.deleteById(1L, 1L, RoleType.ROLE_USER.toString());

        verify(carRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteById_invalidUser_throwsException() {
        Car car = new Car();
        car.setId(1L);
        car.setIdUser(2L); // Different user ID

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(RuntimeException.class, () -> carService.deleteById(1L, 1L, RoleType.ROLE_USER.toString()));
    }

    // Test for getCarsByUserId
    @Test
    void getCarsByUserId_returnsCars() {
        Car car = new Car();
        car.setId(1L);
        car.setIdUser(1L);

        when(carRepository.findByIdUser(1L)).thenReturn(List.of(car));

        List<Car> result = carService.getCarsByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(carRepository, times(1)).findByIdUser(1L);
    }

    // Test for update
    @Test
    void update_validDetails_updatesSuccessfully() {
        Car car = new Car();
        car.setId(1L);
        car.setIdUser(1L);
        car.setBrand("Toyota");

        CarDto carDetails = new CarDto(1L, "Honda", "Civic", 2020, 1.8, "Petrol", "Blue", "Manual", 1L);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        carService.update(1L, carDetails, 1L, RoleType.ROLE_USER.toString());

        assertEquals("Honda", car.getBrand());
        verify(carRepository, times(1)).save(car);
    }

    @Test
    void update_invalidUser_throwsException() {
        Car car = new Car();
        car.setId(1L);
        car.setIdUser(2L); // Different user ID

        CarDto carDetails = new CarDto(1L, "Honda", "Civic", 2020, 1.8, "Petrol", "Blue", "Manual", 2L);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(RuntimeException.class, () -> carService.update(1L, carDetails, 1L, RoleType.ROLE_USER.toString()));
    }
}
