package pl.edu.agh.car_service.Configuration.Authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AuthorizationConfiguration {
    @Bean
    public RestClient authorizationClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestTemplate restTesmplate() {
        return new RestTemplate();
    }
}
