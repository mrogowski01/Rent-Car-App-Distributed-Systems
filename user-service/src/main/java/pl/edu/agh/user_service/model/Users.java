package pl.edu.agh.user_service.model;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.*;

@Entity(name = "users")
@Table(name = "users", schema = "public", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
public class Users {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NonNull
  private String username;

  @NonNull
  private String password;

  @Getter
  @Setter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id")
  private Role role;
}
