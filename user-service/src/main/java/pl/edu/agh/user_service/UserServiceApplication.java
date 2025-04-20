package pl.edu.agh.user_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.agh.user_service.model.ERole;
import pl.edu.agh.user_service.model.Role;
import pl.edu.agh.user_service.model.Users;
import pl.edu.agh.user_service.repository.RoleRepository;
import pl.edu.agh.user_service.repository.UserRepository;

@SpringBootApplication
public class UserServiceApplication {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder encoder;

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

//	@Bean
//	public CommandLineRunner run() {
//		return args -> {
//			if (!userRepository.existsByUsername("adminuser@gmail.com")) {
//				Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Admin role not found"));
//				String encodedPassword = encoder.encode("adminpassword");
//				Users adminUser = new Users("adminuser@gmail.com", encodedPassword);
//				adminUser.setRole(adminRole);
//				userRepository.save(adminUser);
//			}
//		};
//	}
}
