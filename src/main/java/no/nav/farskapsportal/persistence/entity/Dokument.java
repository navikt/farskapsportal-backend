package no.nav.farskapsportal.persistence.entity;

import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Entity
@Validated
@Builder
@Getter
@Setter
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

  @OneToOne(cascade = CascadeType.ALL)
  private RedirectUrl redirectUrlMor;

  @OneToOne(cascade = CascadeType.ALL)
  private RedirectUrl redirectUrlFar;

  private LocalDateTime signertAvMor;

  private LocalDateTime signertAvFar;

  @Override
  public String toString() {
    return "Dokumentnavn: " + dokumentnavn;
  }
}
