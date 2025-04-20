package pl.edu.agh.car_service.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Models.Authorization.AuthorizationDto;

@Service
public class AuthorizationService {
    private final RestClient client;
    private static final String AUTH_SERVICE_URL = "http://localhost:8000/api/auth/";

    @Autowired
    public AuthorizationService(RestClient client) {
        this.client = client;
    }

    public boolean IsAuthorized(String token, RoleType roleType) {
        try {
            var authorizationDto = new AuthorizationDto(token, roleType);
            return Boolean.TRUE.equals(client.post().uri(AUTH_SERVICE_URL)
                            .body(authorizationDto)
                    .retrieve().body(boolean.class));
        } catch (Exception e) {
            return false;
        }
    }
}
