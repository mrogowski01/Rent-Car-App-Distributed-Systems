package pl.edu.agh.car_service.Models.Weather;

import lombok.*;

import java.util.List;

@Data
public class WeatherApiResponse {

    private Location location;
    private Forecast forecast;

    @Data
    public static class Location {
        private String name;
        private String region;
        private String country;
        private double lat;
        private double lon;
        private String tz_id;
        private long localtime_epoch;
        private String localtime;
    }

    @Data
    public static class Forecast {
        private List<ForecastDay> forecastday;

        @Data
        public static class ForecastDay {
            private String date;
            private long date_epoch;
            private Day day;

            @Data
            public static class Day {
                private double maxtemp_c;
                private double maxtemp_f;
                private double mintemp_c;
                private double mintemp_f;
                private double avgtemp_c;
                private double avgtemp_f;
                private double maxwind_mph;
                private double maxwind_kph;
                private double totalprecip_mm;
                private double totalprecip_in;
                private double avgvis_km;
                private double avgvis_miles;
                private double avghumidity;
                private Condition condition;
                private double uv;

                @Data
                public static class Condition {
                    private String text;
                    private String icon;
                    private int code;
                }
            }
        }
    }
}
