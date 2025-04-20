package pl.edu.agh.car_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

 @SpringBootApplication
public class CarServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(CarServiceApplication.class, args);
	}
}
