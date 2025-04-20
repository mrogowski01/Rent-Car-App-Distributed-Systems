package pl.edu.agh.car_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.car_service.Configuration.Authorization.JwtUtils;
import pl.edu.agh.car_service.Helpers.MailHelper;
import pl.edu.agh.car_service.Helpers.OwnerMailProvider;
import pl.edu.agh.car_service.Models.Mail.MailStructure;
import pl.edu.agh.car_service.Models.Offer.AddOfferDto;
import pl.edu.agh.car_service.Models.Reservation.AddReservationDto;
import pl.edu.agh.car_service.Repositories.CarRepository;
import pl.edu.agh.car_service.Repositories.OfferRepository;
import pl.edu.agh.car_service.Repositories.ReservationRepository;
import pl.edu.agh.car_service.Services.ReservationService;

import java.security.Key;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReservationServiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerMailProvider ownerMailProvider;

    @MockitoBean
    private MailHelper mailHelper;

    @InjectMocks
    private ReservationService reservationService;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
        registry.add("user-service.enabled", () -> "false");
    }

    @Test
    void testCreateReservation() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);
        AddReservationDto reservationDto = new AddReservationDto();
        reservationDto.setIdOffer(offerId);
        reservationDto.setDateFrom(LocalDate.now().plusDays(1));
        reservationDto.setDateTo(LocalDate.now().plusDays(5));

        String reservationJson = String.format(
                "{\n" +
                        "\"idOffer\": %d,\n" +
                        "\"dateFrom\": \"%s\",\n" +
                        "\"dateTo\": \"%s\"\n" +
                        "}",
                reservationDto.getIdOffer(), reservationDto.getDateFrom(), reservationDto.getDateTo());

        when(ownerMailProvider.fetchEmailWithBearerToken(anyString(), anyString()))
                .thenReturn("");

        when(mailHelper.sendMail(any(MailStructure.class))).thenReturn(true);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", userToken2)
                        .contentType("application/json")
                        .content(reservationJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

    }

    @Test
    void testGetReservationByIdUnauthorized() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);
        Long reservationId = createReservationForOffer(offerId, userToken2);

        when(ownerMailProvider.fetchEmailWithBearerToken(anyString(), anyString()))
                .thenReturn("");

        when(mailHelper.sendMail(any(MailStructure.class))).thenReturn(true);


        mockMvc.perform(get("/api/reservations/" + reservationId)
                        .header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetReservationByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/reservations/9999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllReservationsByAdmin() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllReservationsByUser() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateReservation() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);
        Long reservationId = createReservationForOffer(offerId, userToken2);

        mockMvc.perform(put("/api/reservations/" + reservationId)
                        .header("Authorization", userToken2)
                        .contentType("application/json")
                        .content("{ \"dateFrom\": \"2025-02-01\", \"dateTo\": \"2025-02-05\", \"idOffer\": " + offerId + " }"))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteReservationAsAdmin() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);
        Long reservationId = createReservationForOffer(offerId, userToken2);

        mockMvc.perform(delete("/api/reservations/" + reservationId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteReservationNotFound() throws Exception {
        Long carId = createCar(2L, userToken);
        Long offerId = createOfferForCar(carId, userToken);
        Long reservationId = createReservationForOffer(offerId, userToken2);

        mockMvc.perform(delete("/api/reservations/9999")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
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

    private Long createReservationForOffer(Long offerId, String token) throws Exception {
        AddReservationDto reservationDto = new AddReservationDto();
        reservationDto.setIdOffer(offerId);
        reservationDto.setDateFrom(LocalDate.now().plusDays(1));
        reservationDto.setDateTo(LocalDate.now().plusDays(5));

        String reservationJson = String.format(
                        "{\n" +
                        "\"idOffer\": %d,\n" +
                        "\"dateFrom\": \"%s\",\n" +
                        "\"dateTo\": \"%s\"\n" +
                        "}",
                reservationDto.getIdOffer(), reservationDto.getDateFrom(), reservationDto.getDateTo());

        when(ownerMailProvider.fetchEmailWithBearerToken(anyString(), anyString()))
                .thenReturn("");

        when(mailHelper.sendMail(any(MailStructure.class))).thenReturn(true);

        String response = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content(reservationJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return Long.parseLong(response);
    }


}
