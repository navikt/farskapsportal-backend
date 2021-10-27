package no.nav.farskapsportal.backend.libs.dto.skatt.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"avsendersMeldingsidentifikator", "avsendersInnsendingstidspunkt", "kildesystem"})
public class Innsending {

  @XmlElement
  private Tekst avsendersMeldingsidentifikator;

  @XmlElement
  private DatoKlokkeslett avsendersInnsendingstidspunkt;

  @XmlElement
  private Tekst kildesystem;

}
