package pl.edu.agh.car_service.Helpers;


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class OwnerMailProvider {

    private final RestTemplate restTemplate;

    public OwnerMailProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String fetchEmailWithBearerToken(String url, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to fetch email: " + e.getMessage());
        }
    }
}
