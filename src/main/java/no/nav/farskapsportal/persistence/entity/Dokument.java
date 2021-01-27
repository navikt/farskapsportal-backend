package no.nav.farskapsportal.persistence.entity;

import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.validation.annotation.Validated;

@Entity
@Validated
@Builder
@Getter
@Setter
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
public class Dokument implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String dokumentnavn;

  private byte[] innhold;

  private URI dokumentStatusUrl;

  private URI padesUrl;

  private URI redirectUrlMor;

  private URI redirectUrlFar;

  private LocalDateTime signertAvMor;

  private LocalDateTime signertAvFar;

  @Override
  public String toString() {
    return "Dokumentnavn: " + dokumentnavn;
  }
}
