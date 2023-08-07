package no.nav.farskapsportal.backend.libs.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Signeringsinformasjon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String statusSignering;

  private LocalDateTime signeringstidspunkt;

  private LocalDateTime sendtTilSignering;

  private String redirectUrl;

  private String undertegnerUrl;

  @Column(length = 100000000)
  private byte[] xadesXml;
}
