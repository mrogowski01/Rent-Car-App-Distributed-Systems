package pl.edu.agh.car_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.car_service.Controllers.CarController;

import java.security.Key;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CarServiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private static String adminToken;
    private static String userToken;
    private static String userToken2;
    private Key secretKey;

    @Value("${car_service.app.jwtSecret}")
    private String jwtSecret;
    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

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
    static void postgresProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void testGetAllCarsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/cars")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetAllCarsAsUnauthorizedUser() throws Exception {
        mockMvc.perform(get("/api/cars")
                        .header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateCar() throws Exception {
        String newCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }""";

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCarJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cars/user")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Toyota"))
                .andExpect(jsonPath("$[0].model").value("Corolla"))
                .andExpect(jsonPath("$[0].prod_year").value("2021"))
                .andExpect(jsonPath("$[0].engine").value(1.8))
                .andExpect(jsonPath("$[0].fuel_type").value("Gasoline"))
                .andExpect(jsonPath("$[0].color").value("White"))
                .andExpect(jsonPath("$[0].gear_type").value("Automatic"));
    }

    @Test
    void testUpdateCarByUser() throws Exception {
        String newCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }
                """;

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCarJson))
                .andExpect(status().isOk());

        String updatedCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Avensis",
                    "prod_year": 2022,
                    "engine": 2.0,
                    "fuel_type": "Diesel",
                    "color": "Black",
                    "gear_type": "Automatic"
                }
                """;

        mockMvc.perform(put("/api/cars/1")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedCarJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cars/user")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].model").value("Avensis"))
                .andExpect(jsonPath("$[0].prod_year").value("2022"))
                .andExpect(jsonPath("$[0].engine").value(2.0))
                .andExpect(jsonPath("$[0].fuel_type").value("Diesel"))
                .andExpect(jsonPath("$[0].color").value("Black"));
    }

    @Test
    void testUpdateCarByAdmin() throws Exception {
        String newCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }
                """;

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCarJson))
                .andExpect(status().isOk());

        String updatedCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Avensis",
                    "prod_year": 2022,
                    "engine": 2.0,
                    "fuel_type": "Diesel",
                    "color": "Black",
                    "gear_type": "Automatic"
                }
                """;

        mockMvc.perform(put("/api/cars/1")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedCarJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cars/user")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].model").value("Avensis"))
                .andExpect(jsonPath("$[0].prod_year").value("2022"))
                .andExpect(jsonPath("$[0].engine").value(2.0))
                .andExpect(jsonPath("$[0].fuel_type").value("Diesel"))
                .andExpect(jsonPath("$[0].color").value("Black"));
    }

    @Test
    void testGetAllExistingCarsAsAdmin() throws Exception {
        String newCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }""";

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCarJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cars")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Toyota"))
                .andExpect(jsonPath("$[0].model").value("Corolla"))
                .andExpect(jsonPath("$[0].prod_year").value("2021"))
                .andExpect(jsonPath("$[0].engine").value(1.8))
                .andExpect(jsonPath("$[0].fuel_type").value("Gasoline"))
                .andExpect(jsonPath("$[0].color").value("White"))
                .andExpect(jsonPath("$[0].gear_type").value("Automatic"));
    }

    @Test
    void testDeleteCarAsAdmin() throws Exception {
        String newCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }
                """;

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCarJson))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cars/1")
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteCarAsUnauthorizedUser() throws Exception {
        String newCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }
                """;

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCarJson))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cars/1")
                        .header("Authorization", userToken2))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteCarAsCarOwner() throws Exception {
        String newCarJson = """
                {
                    "brand": "Toyota",
                    "model": "Corolla",
                    "prod_year": 2021,
                    "engine": 1.8,
                    "fuel_type": "Gasoline",
                    "color": "White",
                    "gear_type": "Automatic"
                }
                """;

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCarJson))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cars/1")
                        .header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }
}
