package no.nav.farskapsportal.backend.libs.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.io.Serializable;
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

  private String navn;

  private String tittel;

  @Column(unique = true)
  private String jobbref;

  @OneToOne(cascade = CascadeType.ALL)
  private Dokumentinnhold dokumentinnhold;

  private String statusUrl;

  private String statusQueryToken;

  private String padesUrl;

  @Column(name = "blob_id_pades")
  @OneToOne(cascade = CascadeType.ALL)
  private BlobIdGcp blobIdPades;

  private String bekreftelsesUrl;

  @OneToOne(cascade = CascadeType.ALL)
  private Signeringsinformasjon signeringsinformasjonMor;

  @OneToOne(cascade = CascadeType.ALL)
  private Signeringsinformasjon signeringsinformasjonFar;

  @Override
  public String toString() {
    return "Dokumentnavn: " + navn;
  }
}
