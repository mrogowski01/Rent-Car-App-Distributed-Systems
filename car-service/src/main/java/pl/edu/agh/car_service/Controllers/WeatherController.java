package pl.edu.agh.car_service.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import pl.edu.agh.car_service.Configuration.Authorization.JwtUtils;
import pl.edu.agh.car_service.Entities.Car;
import pl.edu.agh.car_service.Entities.Offer;
import pl.edu.agh.car_service.Mappers.ReservationMapper;
import pl.edu.agh.car_service.Models.Response.ResponseMessage;
import pl.edu.agh.car_service.Models.Weather.ReservationWithWeatherDto;
import pl.edu.agh.car_service.Models.Weather.WeatherApiResponse;
import pl.edu.agh.car_service.Models.Weather.WeatherDto;
import pl.edu.agh.car_service.Models.Weather.WeatherForecastDto;
import pl.edu.agh.car_service.Repositories.CarRepository;
import pl.edu.agh.car_service.Repositories.OfferRepository;
import pl.edu.agh.car_service.Repositories.ReservationRepository;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    public static final String WEATHER_ENDPOINT_URL = "https://api.open-meteo.com/v1/forecast?longitude=%f&latitude=%f&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m&timezone=auto";
    private final RestClient restClient;
    private final ReservationRepository reservationRepository;
    private final OfferRepository offerRepository;
    private final CarRepository carRepository;
    private final RestTemplate restTemplate = new RestTemplate();;
    private final JwtUtils jwtUtils;

    private static final String WEATHER_API_KEY = "db9a65916a0a415ea0074907253001";
    private static final String WEATHER_API_URL = "http://api.weatherapi.com/v1/future.json?key=%s&q=Cracow&dt=%s";


    @Autowired
    public WeatherController(RestClient restClient, ReservationRepository reservationRepository, OfferRepository offerRepository, CarRepository carRepository, JwtUtils jwtUtils) {
        this.restClient = restClient;
        this.reservationRepository = reservationRepository;
        this.offerRepository = offerRepository;
        this.carRepository = carRepository;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping
    public ResponseEntity<?> GetWeather(@RequestParam double longitude, @RequestParam double latitude) {
        try {
            var response = restClient.get().uri(String.format(WEATHER_ENDPOINT_URL, longitude, latitude))
                    .retrieve()
                    .body(WeatherDto.WeatherResponse.class);

                return ResponseEntity.ok().body(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Error fetching the weather API: " + ex.getMessage());
        }
    }

    @GetMapping("/user/reservations-with-forecast")
    public ResponseEntity<?> getReservationsWithForecastByUserId(@RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtils.getUserId(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<ReservationWithWeatherDto> reservationsWithWeather = reservationRepository.findByIdUser(userId).stream()
                    .map(reservation -> {
                        Offer offer = offerRepository.findByIdOffer(reservation.getIdOffer()).orElse(null);
                        if (offer != null) {
                            Car car = carRepository.findById(offer.getCarId()).orElse(null);
                            offer.setCarDetails(car);
                        }
                        reservation.setOfferDetails(offer);

                        WeatherForecastDto startDateWeather = fetchWeatherForDate(reservation.getDateFrom());
                        WeatherForecastDto endDateWeather = fetchWeatherForDate(reservation.getDateTo());

                        return new ReservationWithWeatherDto(ReservationMapper.ReservationToReservationDto(reservation),
                                startDateWeather, endDateWeather);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reservationsWithWeather);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Error fetching reservations with weather: " + ex.getMessage()));
        }
    }

    private WeatherForecastDto fetchWeatherForDate(LocalDate date) {
        try {
            String weatherApiUrl = String.format(WEATHER_API_URL, WEATHER_API_KEY, date);
            WeatherApiResponse response = restTemplate.getForObject(weatherApiUrl, WeatherApiResponse.class);

            if (response != null && response.getForecast() != null && !response.getForecast().getForecastday().isEmpty()) {
                var day = response.getForecast().getForecastday().getFirst().getDay();
                return new WeatherForecastDto(day.getMaxtemp_c(), day.getMintemp_c(), day.getCondition().getText());
            }
        } catch (Exception ex) {
            System.err.println("Error fetching weather data for date " + date + ": " + ex.getMessage());
        }
        return null;
    }
}
