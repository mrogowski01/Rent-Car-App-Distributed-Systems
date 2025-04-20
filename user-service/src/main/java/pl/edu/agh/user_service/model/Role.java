package pl.edu.agh.user_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles", schema = "public")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Getter
  @Setter
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private ERole name;
}
