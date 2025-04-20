package pl.edu.agh.car_service.Entities;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String brand;
    private String model;
    private int prod_year;
    private Double engine;
    private String fuel_type;
    private String color;
    private String gear_type;

    @Column(name = "id_user")
    private Long idUser;
}