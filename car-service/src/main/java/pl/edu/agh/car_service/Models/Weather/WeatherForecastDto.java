package pl.edu.agh.car_service.Models.Weather;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class WeatherForecastDto {
    private Double maxTempC;
    private Double minTempC;
    private String conditionText;
}