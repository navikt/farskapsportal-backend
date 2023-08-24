package no.nav.farskapsportal.backend.libs.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlobIdGcp {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String bucket;
  private String name;
  private Long generation;
}
