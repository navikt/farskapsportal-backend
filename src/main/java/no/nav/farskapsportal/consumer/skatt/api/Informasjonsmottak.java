package no.nav.farskapsportal.consumer.skatt.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
@XmlType( propOrder = {"informasjonskanal", "mottakstidspunktFraOpprinneligkanal" ,"puncher"})
public class Informasjonsmottak {

  @XmlElement
  private KanalForRegistreringAvFarskap informasjonskanal;

  @XmlElement
  private Dato mottakstidspunktFraOpprinneligkanal;

  @XmlElement
  private NorskIdentifikator puncher;

}
