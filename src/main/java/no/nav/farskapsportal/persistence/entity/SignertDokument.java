package no.nav.farskapsportal.persistence.entity;

import java.io.Serializable;
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
public class SignertDokument implements Serializable  {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  @NotNull
  private String dokumentnavn;

  @NotNull
  private byte[] signertDokument;

  @OneToOne(mappedBy = "signertErklaering", cascade = CascadeType.ALL)
  private Farskapserklaering signerterklaering;

  @Override
  public String toString() {
    return "Dokumentnavn: " + dokumentnavn;
  }

}
