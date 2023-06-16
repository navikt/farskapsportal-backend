package no.nav.farskapsportal.backend.libs.dto.skatt.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class Mor {

  @XmlElement
  private Foedselsnummer foedselsEllerDNummer;

  @XmlElement
  private Boolsk harSignert;


}
