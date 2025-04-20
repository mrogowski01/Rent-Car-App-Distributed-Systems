package pl.edu.agh.car_service.Controllers;

import pl.edu.agh.car_service.Configuration.Authorization.JwtUtils;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Models.Car.AddCarDto;
import pl.edu.agh.car_service.Models.Car.CarDto;
import pl.edu.agh.car_service.Models.Response.ResponseMessage;
import pl.edu.agh.car_service.Services.CarService;
import pl.edu.agh.car_service.Services.OfferService;
import pl.edu.agh.car_service.Services.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.car_service.Entities.Car;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/cars")
public class CarController {
    private final CarService carService;
    private final OfferService offerService;
    private final ReservationService reservationService;
    private final JwtUtils jwtUtils;

    @Autowired
    public CarController(CarService carService, OfferService offerService, ReservationService reservationService, JwtUtils jwtUtils) {
        this.carService = carService;
        this.offerService = offerService;
        this.reservationService = reservationService;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping
    public ResponseEntity<List<CarDto>> getAllCars(@RequestHeader("Authorization") String token) {
        String role = jwtUtils.getRole(token);
        if (!role.equals(RoleType.ROLE_ADMIN.toString()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var cars = carService.findAll();
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarDto> getCarById(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String role = jwtUtils.getRole(token);

        var car = carService.findById(id);
        if (car == null)
            return ResponseEntity.notFound().build();

        if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !userId.equals(car.getIdUser()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return ResponseEntity.ok(car);
    }

    @PostMapping
    public ResponseEntity<Long> createCar(@RequestHeader("Authorization") String token, @RequestBody AddCarDto car) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Long savedCarId = carService.save(car, userId);
        return ResponseEntity.ok(savedCarId);
    }

    @GetMapping("/user")
    public ResponseEntity<List<Car>> getCarsByUserId(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Car> cars = carService.getCarsByUserId(userId);
        return new ResponseEntity<>(cars, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> updateCar(@RequestHeader("Authorization") String token, @PathVariable Long id, @RequestBody CarDto carDetails) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String role = jwtUtils.getRole(token);

        try {
            carService.update(id, carDetails, userId, role);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Error during modifying car: " + ex.getMessage()));
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> deleteCar(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String role = jwtUtils.getRole(token);
        if (!role.equals(RoleType.ROLE_ADMIN.toString()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var car = carService.findById(id);
        if (car == null)
            return ResponseEntity.notFound().build();

        try {
            reservationService.deleteReservationsByCarId(id, userId, role);
            offerService.deleteOffersByCarId(id, userId, role);
            carService.deleteById(id, userId, role);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Error during deleting: " + ex.getMessage()));
        }
    }
}