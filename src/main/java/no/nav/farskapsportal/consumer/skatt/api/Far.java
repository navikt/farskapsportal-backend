package no.nav.farskapsportal.consumer.skatt.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = {"foedselsEllerDNummer", "datoForErklaeringen", "harSignert"})
public class Far {

  @XmlElement
  private Foedselsnummer foedselsEllerDNummer;

  @XmlElement
  private Dato datoForErklaeringen;

  @XmlElement
  private Boolsk harSignert;

}
