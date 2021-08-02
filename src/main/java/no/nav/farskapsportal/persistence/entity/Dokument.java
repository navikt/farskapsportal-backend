package no.nav.farskapsportal.persistence.entity;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.farskapsportal.api.StatusSignering;
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

  @OneToOne(cascade = CascadeType.ALL)
  private Dokumentinnhold dokumentinnhold;

  private String dokumentStatusUrl;

  private String padesUrl;

  private String bekreftelsesUrl;

  @OneToOne(cascade = CascadeType.ALL)
  private Signeringsinformasjon signeringsinformasjonMor;

  @OneToOne(cascade = CascadeType.ALL)
  private Signeringsinformasjon signeringsinformasjonFar;

  @Override
  public String toString() {
    return "Dokumentnavn: " + dokumentnavn;
  }
}
