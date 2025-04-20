package pl.edu.agh.car_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.car_service.Models.Offer.AddOfferDto;
import pl.edu.agh.car_service.Models.Car.CarDto;

import java.security.Key;
import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OfferServiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private static String adminToken;
    private static String userToken;
    private static String userToken2;

    @Value("${car_service.app.jwtSecret}")
    private String jwtSecret;

    private static String generateToken(Long userId, String username, String role, Key key) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @BeforeAll
    static void beforeAll(@Value("${car_service.app.jwtSecret}") String jwtSecret) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        adminToken = generateToken(1L, "adminUser", "ROLE_ADMIN", key);
        userToken = generateToken(2L, "normalUser", "ROLE_USER", key);
        userToken2 = generateToken(3L, "normalUser2", "ROLE_USER", key);
    }

    @Container
    private static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:latest")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpassword");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void testCreateOffer() throws Exception {
        Long carId = createCar(2L, userToken);

        AddOfferDto offerDto = new AddOfferDto();
        offerDto.setCarId(carId);
        offerDto.setAvailableFrom(LocalDate.now().plusDays(1));
        offerDto.setAvailableTo(LocalDate.now().plusDays(10));
        offerDto.setPrice(100L);

        mockMvc.perform(post("/api/offers")
                        .header("Authorization", userToken)
                        .contentType("application/json")
                        .content("{ \"carId\": " + carId + ", \"availableFrom\": \"2025-01-29\", \"availableTo\": \"2025-02-07\", \"price\": 100 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testGetOfferByIdUnauthorized() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);

        mockMvc.perform(get("/api/offers/" + offerId)
                    .header("Authorization", userToken2))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetOfferByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/offers/9999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllOffersByAdmin() throws Exception {
        mockMvc.perform(get("/api/offers")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllOffersByUser() throws Exception {
        mockMvc.perform(get("/api/offers")
                        .header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateOffer() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);
        String updatePayload = String.format(
                """
                {
                    "id": %d,
                    "carId": %d,
                    "price": 150,
                    "availableFrom": "2025-02-01",
                    "availableTo": "2025-02-10"
                }
                """,
                offerId, carId
        );

        mockMvc.perform(put("/api/offers/" + offerId)
                        .header("Authorization", userToken)
                        .contentType("application/json")
                        .content(updatePayload))
                .andExpect(status().isOk());
    }


    @Test
    void testDeleteOfferByAdmin() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);

        mockMvc.perform(delete("/api/offers/" + offerId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteOfferByUser() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);

        mockMvc.perform(delete("/api/offers/" + offerId)
                        .header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteOfferNotFoundByAdmin() throws Exception {
        mockMvc.perform(delete("/api/offers/9999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetOffersByCarId() throws Exception {
        Long carId = createCar(2L, userToken);

        mockMvc.perform(get("/api/offers/car/" + carId)
                        .header("Authorization", userToken))
                .andExpect(status().isOk());
    }

    private Long createCar(Long userId, String token) throws Exception {
        String carJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }""";
        String response = mockMvc.perform(post("/api/cars")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return Long.parseLong(response);
    }

    private Long createOfferForCar(Long carId, String token) throws Exception {
        AddOfferDto offerDto = new AddOfferDto();
        offerDto.setCarId(carId);
        offerDto.setAvailableFrom(LocalDate.now().plusDays(1));
        offerDto.setAvailableTo(LocalDate.now().plusDays(10));
        offerDto.setPrice(100L);

        String response = mockMvc.perform(post("/api/offers")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content("{ \"carId\": " + carId + ", \"availableFrom\": \"2025-01-29\", \"availableTo\": \"2025-02-07\", \"price\": 100 }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return Long.parseLong(response);
    }
}
