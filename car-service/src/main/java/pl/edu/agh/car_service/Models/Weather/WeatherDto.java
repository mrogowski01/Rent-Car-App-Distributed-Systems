package pl.edu.agh.car_service.Models.Weather;

import java.util.List;

public class WeatherDto {
    public record WeatherResponse(
            double latitude,
            double longitude,
            double generationtime_ms,
            int utc_offset_seconds,
            String timezone,
            String timezone_abbreviation,
            double elevation,
            HourlyUnits hourly_units,
            Hourly hourly
    ) {}

    public record HourlyUnits(
            String time,
            String temperature_2m,
            String relative_humidity_2m,
            String wind_speed_10m
    ) {}

    public record Hourly(
            List<String> time,
            List<Double> temperature_2m,
            List<Integer> relative_humidity_2m,
            List<Double> wind_speed_10m
    ) {}
}