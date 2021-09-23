package no.nav.farskapsportal.backend.lib.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
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
public class Signeringsinformasjon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String statusSignering;

  private LocalDateTime signeringstidspunkt;

  private String redirectUrl;

  private String undertegnerUrl;

  private String xadesUrl;

  @Column(length = 100000000)
  private byte[] xadesXml;

}
