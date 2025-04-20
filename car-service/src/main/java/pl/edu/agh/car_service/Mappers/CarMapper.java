package pl.edu.agh.car_service.Mappers;

import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Models.Car.AddCarDto;
import pl.edu.agh.car_service.Models.Car.CarDto;

public class CarMapper {

    public static Car CarDtoToCar(CarDto carDto) {
        return new Car(carDto.getId(),
                carDto.getBrand(),
                carDto.getModel(),
                carDto.getProd_year(),
                carDto.getEngine(),
                carDto.getFuel_type(),
                carDto.getColor(),
                carDto.getGear_type(),
                carDto.getIdUser());
    }

    public static CarDto CarToCarDto(Car car) {
        return new CarDto(car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getProd_year(),
                car.getEngine(),
                car.getFuel_type(),
                car.getColor(),
                car.getGear_type(),
                car.getIdUser());
    }

    public static Car AddCarDtoToCar(AddCarDto carDto, Long userId) {
        return new Car(null,
                carDto.getBrand(),
                carDto.getModel(),
                carDto.getProd_year(),
                carDto.getEngine(),
                carDto.getFuel_type(),
                carDto.getColor(),
                carDto.getGear_type(),
                userId);
    }
}
