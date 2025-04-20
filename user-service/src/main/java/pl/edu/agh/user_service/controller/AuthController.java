package pl.edu.agh.user_service.controller;

import jakarta.validation.Valid;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import pl.edu.agh.user_service.model.Dtos.PermissionDto;
import pl.edu.agh.user_service.model.ERole;
import pl.edu.agh.user_service.model.Role;
import pl.edu.agh.user_service.model.Users;
import pl.edu.agh.user_service.payload.request.LoginRequest;
import pl.edu.agh.user_service.payload.request.SignupRequest;
import pl.edu.agh.user_service.payload.response.JwtResponse;
import pl.edu.agh.user_service.payload.response.MessageResponse;
import pl.edu.agh.user_service.repository.RoleRepository;
import pl.edu.agh.user_service.repository.UserRepository;
import pl.edu.agh.user_service.security.jwt.JwtUtils;
import pl.edu.agh.user_service.security.services.UserDetailsImpl;
import pl.edu.agh.validators.UserValidator;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (jwtUtils.validateJwtToken(jwt)) {
            return ResponseEntity.ok("Token is valid");
        } else {
            return ResponseEntity.status(401).body("Invalid token");
        }
    }

    @GetMapping("/users/{userId}/email")
    public ResponseEntity<String> getUserEmailById(@RequestHeader("Authorization") String token, @PathVariable Long userId) {
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        Long uId = jwtUtils.getUserIdFromJwtToken(jwt);
        System.out.println(uId);
        if (uId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Users> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Users user = userOptional.get();
        System.out.println(user.getUsername());
        return ResponseEntity.ok(user.getUsername());
    }

    @PostMapping("/giveAdmin")
    public ResponseEntity<?> giveAdminAccess(@RequestHeader("Authorization") String token, @RequestBody PermissionDto userEmail) {
        boolean isAdmin = false;
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtils.getRoleFromJwtToken(jwt);
        Long adminId = jwtUtils.getUserIdFromJwtToken(jwt);

        if (!role.equals(ERole.ROLE_ADMIN.toString()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(401, "User does not have permission to give ADMIN access"));

        Users user;
        try {
            user = userRepository.findByUsername(userEmail.getUserMail())
                    .orElseThrow(() -> new RuntimeException("Error: E-mail is not found."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(400, "Error: E-mail is not found."));
        }

        log.info(user.getRole().getName().toString());
        if (user.getId().equals(adminId))
            return ResponseEntity.badRequest().body(new MessageResponse(400, "You already have the role ADMIN"));

        if (user.getRole().getName().equals(ERole.ROLE_ADMIN))
            return ResponseEntity.badRequest().body(new MessageResponse(400, "User already has the role ADMIN"));

        Role userRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.setRole(userRole);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse(200, "ADMIN role added to user: " + userEmail.getUserMail()));
    }

    @PostMapping("/removeAdmin")
    public ResponseEntity<?> removeAdminAccess(@RequestHeader("Authorization") String token, @RequestBody PermissionDto userEmail) {
        boolean isAdmin = false;
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtils.getRoleFromJwtToken(jwt);
        Long adminId = jwtUtils.getUserIdFromJwtToken(jwt);

        if (!role.equals(ERole.ROLE_ADMIN.toString()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(401,"User does not have permission to remove ADMIN access"));

        Users user;
        try {
            user = userRepository.findByUsername(userEmail.getUserMail())
                    .orElseThrow(() -> new RuntimeException("Error: E-mail is not found."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(400, "Error: E-mail is not found."));
        }

        if (adminId.equals(user.getId()))
            return ResponseEntity.badRequest().body(new MessageResponse(400, "Cannot remove your own ADMIN role"));

        log.info(user.getRole().getName().toString());
        if (user.getRole().getName().equals(ERole.ROLE_USER))
            return ResponseEntity.badRequest().body(new MessageResponse(400, "User already has the role USER"));

        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.setRole(userRole);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse(200, "USER role added to user: " + userEmail.getUserMail()));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody(required = false) LoginRequest loginRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        String username;
        String password;

        // basic auth
        if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
            String base64Credentials = authorizationHeader.substring("Basic ".length());
            String credentials = new String(Base64.decodeBase64(base64Credentials));
            String[] values = credentials.split(":", 2);
            username = values[0];
            password = values[1];
        } else if (loginRequest != null) {
            // from body
            username = loginRequest.getUsername();
            password = loginRequest.getPassword();
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse(400, "Missing credentials"));
        }

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(username, password));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream().findFirst().orElseThrow().getAuthority();

        return ResponseEntity.ok(new JwtResponse("Login successful", jwt, userDetails.getId(), userDetails.getUsername(), role));
    }


    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse(400, "Error: E-mail is already taken!"));
        }

        var validationResult = UserValidator.validateSignupRequest(signUpRequest);
        if (validationResult != null)
            return ResponseEntity.badRequest().body(new MessageResponse(400, validationResult));

        Users user = new Users(signUpRequest.getUsername(), encoder.encode(signUpRequest.getPassword()));
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.setRole(userRole);

        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse(200, "User registered successfully!"));
    }
}
