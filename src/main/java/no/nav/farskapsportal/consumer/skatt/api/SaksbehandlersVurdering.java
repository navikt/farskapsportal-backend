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
@XmlType(propOrder = {"farsForeldreHarSignert", "skjemaErAttestert", "vedlagtFarskapsskjemaErOriginalt", "manuellBehandlingErOensket",
    "kommentarForManuellBehandling"})
public class SaksbehandlersVurdering {

  @XmlElement
  private Boolsk farsForeldreHarSignert;

  @XmlElement
  private Boolsk skjemaErAttestert;

  @XmlElement
  private Boolsk vedlagtFarskapsskjemaErOriginalt;

  @XmlElement
  private Boolsk manuellBehandlingErOensket;

  @XmlElement
  private Tekst kommentarForManuellBehandling;

}
