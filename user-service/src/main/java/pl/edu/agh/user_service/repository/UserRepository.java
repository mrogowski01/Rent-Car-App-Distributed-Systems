package pl.edu.agh.user_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pl.edu.agh.user_service.model.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
  Optional<Users> findByUsername(String username);

  Boolean existsByUsername(String username);
  Optional<Users> findById(Long id);
  Optional<Users> findByUsernameAndPassword(String username, String password);
}
