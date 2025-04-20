package pl.edu.agh.car_service.Models.Weather;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.agh.car_service.Models.Reservation.ReservationDto;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ReservationWithWeatherDto {
    private ReservationDto reservation;
    private WeatherForecastDto startDateWeather;
    private WeatherForecastDto endDateWeather;
}
