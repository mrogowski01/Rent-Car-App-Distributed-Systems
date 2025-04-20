package pl.edu.agh.car_service.Models.Car;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CarDto {
    private Long id;
    private String brand;
    private String model;
    private int prod_year;
    private Double engine;
    private String fuel_type;
    private String color;
    private String gear_type;
    private Long idUser;
}
