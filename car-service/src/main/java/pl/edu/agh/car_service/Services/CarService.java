package pl.edu.agh.car_service.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Mappers.CarMapper;
import pl.edu.agh.car_service.Models.Car.AddCarDto;
import pl.edu.agh.car_service.Models.Car.CarDto;
import pl.edu.agh.car_service.Repositories.CarRepository;

import java.util.List;
import java.util.Objects;

@Service
public class CarService {
    private final CarRepository carRepository;

    @Autowired
    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public List<CarDto> findAll() {
        var cars = carRepository.findAll();
        return cars.stream().map(CarMapper::CarToCarDto).toList();
    }

    public CarDto findById(Long id) {
        return CarMapper.CarToCarDto(Objects.requireNonNull(carRepository.findById(id).orElse(null)));
    }

    public Long save(AddCarDto carDto, Long userId) {
        Car car = CarMapper.AddCarDtoToCar(carDto, userId);
        return carRepository.save(car).getId();
    }

    public void deleteById(Long id, Long userId, String role) {
        var car = carRepository.findById(id);
        if (car.isPresent())
        {
            if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !car.get().getIdUser().equals(userId))
                throw new RuntimeException("The car does not belong to the user.");

            carRepository.deleteById(id);
        }
    }


    public List<Car> getCarsByUserId(Long userId) {
        return carRepository.findByIdUser(userId);
    }

    public void update(Long id, CarDto carDetails, Long userId, String role) {
        var optionalCar = carRepository.findById(id);
        if (optionalCar.isPresent()) {
            Car car = optionalCar.get();
            if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !car.getIdUser().equals(userId))
                throw new RuntimeException("The car does not belong to the user.");

            car.setBrand(carDetails.getBrand());
            car.setModel(carDetails.getModel());
            car.setProd_year(carDetails.getProd_year());
            car.setEngine(carDetails.getEngine());
            car.setFuel_type(carDetails.getFuel_type());
            car.setColor(carDetails.getColor());
            car.setGear_type(carDetails.getGear_type());
            carRepository.save(car);
        }
    }
}