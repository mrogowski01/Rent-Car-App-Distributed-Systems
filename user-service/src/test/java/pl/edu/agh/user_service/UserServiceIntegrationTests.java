package pl.edu.agh.user_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.github.dockerjava.core.MediaType;
import pl.edu.agh.user_service.model.ERole;
import pl.edu.agh.user_service.model.Role;
import pl.edu.agh.user_service.model.Users;
import pl.edu.agh.user_service.payload.request.LoginRequest;
import pl.edu.agh.user_service.payload.request.SignupRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import pl.edu.agh.user_service.repository.RoleRepository;
import pl.edu.agh.user_service.repository.UserRepository;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserServiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    private static String adminToken;
    private static String userToken;
    private static String userToken2;

    @Value("${user_service.app.jwtSecret}")
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
    static void beforeAll(@Value("${user_service.app.jwtSecret}") String jwtSecret) {
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

    @BeforeEach
    public void setUp() {
        // Insert roles manually if not using data.sql
        jdbcTemplate.update("INSERT INTO roles (name) VALUES ('ROLE_USER')");
        jdbcTemplate.update("INSERT INTO roles (name) VALUES ('ROLE_ADMIN')");
    }

    @Test
    void testRegisterUser() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser@example.com");
        request.setPassword("SecurePass123");

        String registerJson =  objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    @Test
    void testRegisterDuplicateUser() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("duplicate@example.com");
        request.setPassword("SecurePass123");
        String registerJson =  objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: E-mail is already taken!"));
    }

    @Test
    void testAuthenticateUser() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("authuser@example.com");
        signupRequest.setPassword("SecurePass123");
        String registerJson =  objectMapper.writeValueAsString(signupRequest);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("authuser@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson =  objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")));
    }

    @Test
    void testValidateToken() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("validatetoken@example.com");
        signupRequest.setPassword("SecurePass123");
        String registerJson =  objectMapper.writeValueAsString(signupRequest);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("validatetoken@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson =  objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " +  token);

        mockMvc.perform(post("/api/auth/validate-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Token is valid"));
    }

    @Test
    void testGiveAdminAccess() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        SignupRequest userRequest = new SignupRequest();
        userRequest.setUsername("user@example.com");
        userRequest.setPassword("SecurePass123");
        String userJson = objectMapper.writeValueAsString(userRequest);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(userJson))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + adminToken);

        String permissionDto = "{\"userMail\": \"user@example.com\"}";
        mockMvc.perform(post("/api/auth/giveAdmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ADMIN role added to user: user@example.com"));
    }

    @Test
    void testGiveAdminAccessByUserToUser() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("user1@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        SignupRequest userRequest = new SignupRequest();
        userRequest.setUsername("user@example.com");
        userRequest.setPassword("SecurePass123");
        String userJson = objectMapper.writeValueAsString(userRequest);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(userJson))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String userToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + userToken);

        String permissionDto = "{\"userMail\": \"user1@example.com\"}";
        mockMvc.perform(post("/api/auth/giveAdmin")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User does not have permission to give ADMIN access"));
    }

    @Test
    void testGiveAdminAccessForAdminSelf() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + adminToken);

        String permissionDto = "{\"userMail\": \"admin@example.com\"}";
        mockMvc.perform(post("/api/auth/giveAdmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You already have the role ADMIN"));
    }

    @Test
    void testGiveAdminAccessToAdmin() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        Users adminUser2 = new Users();
        adminUser2.setUsername("admin2@example.com");
        adminUser2.setPassword(encoder.encode("SecurePass123"));
        adminUser2.setRole(adminRole);
        userRepository.save(adminUser2);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + adminToken);

        String permissionDto = "{\"userMail\": \"admin2@example.com\"}";
        mockMvc.perform(post("/api/auth/giveAdmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already has the role ADMIN"));
    }

    @Test
    void testGiveAdminAccessToUnknownUser() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + adminToken);

        String permissionDto = "{\"userMail\": \"adminadmiadminadmiand@example.com\"}";
        mockMvc.perform(post("/api/auth/giveAdmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: E-mail is not found."));
    }

    @Test
    void testRemoveAdminAccess() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        Users adminUser2 = new Users();
        adminUser2.setUsername("admin2@example.com");
        adminUser2.setPassword(encoder.encode("SecurePass123"));
        adminUser2.setRole(adminRole);
        userRepository.save(adminUser2);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        LoginRequest loginRequest2 = new LoginRequest();
        loginRequest2.setUsername("admin2@example.com");
        loginRequest2.setPassword("SecurePass123");
        String loginJson2 = objectMapper.writeValueAsString(loginRequest2);
        MvcResult result2 = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson2))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + adminToken);

        String permissionDto = "{\"userMail\": \"admin2@example.com\"}";
        mockMvc.perform(post("/api/auth/removeAdmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("USER role added to user: admin2@example.com"));
    }

    @Test
    void testRemoveAdminAccessWithoutPermission() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        SignupRequest userRequest = new SignupRequest();
        userRequest.setUsername("user@example.com");
        userRequest.setPassword("SecurePass123");
        String userJson = objectMapper.writeValueAsString(userRequest);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(userJson))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String userToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + userToken);

        String permissionDto = "{\"userMail\": \"admin@example.com\"}";
        mockMvc.perform(post("/api/auth/removeAdmin")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User does not have permission to remove ADMIN access"));
    }

    @Test
    void testRemoveAdminAccessSelfRoleAdmin() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + adminToken);

        String permissionDto = "{\"userMail\": \"admin@example.com\"}";
        mockMvc.perform(post("/api/auth/removeAdmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot remove your own ADMIN role"));
    }

    @Test
    void testRemoveAdminAccessToUserWhoDoNotHaveAdminRole() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        SignupRequest userRequest = new SignupRequest();
        userRequest.setUsername("user@example.com");
        userRequest.setPassword("SecurePass123");
        String userJson = objectMapper.writeValueAsString(userRequest);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(userJson))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String userToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + userToken);

        String permissionDto = "{\"userMail\": \"user@example.com\"}";
        mockMvc.perform(post("/api/auth/removeAdmin")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already has the role USER"));
    }

    @Test
    void testRemoveAdminAccessToUnknownUser() throws Exception {
        Users adminUser = new Users();
        adminUser.setUsername("admin@example.com");
        adminUser.setPassword(encoder.encode("SecurePass123"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("SecurePass123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login successful")))
                .andReturn();

        String adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        System.out.println("Token: " + adminToken);

        String permissionDto = "{\"userMail\": \"adminadmiadminadmiand@example.com\"}";
        mockMvc.perform(post("/api/auth/removeAdmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(permissionDto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: E-mail is not found."));
    }


}
