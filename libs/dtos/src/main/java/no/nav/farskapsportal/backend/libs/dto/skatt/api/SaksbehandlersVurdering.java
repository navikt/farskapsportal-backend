package no.nav.farskapsportal.backend.libs.dto.skatt.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = {
      "farsForeldreHarSignert",
      "skjemaErAttestert",
      "vedlagtFarskapsskjemaErOriginalt",
      "manuellBehandlingErOensket",
      "kommentarForManuellBehandling"
    })
public class SaksbehandlersVurdering {

  @XmlElement private Boolsk farsForeldreHarSignert;

  @XmlElement private Boolsk skjemaErAttestert;

  @XmlElement private Boolsk vedlagtFarskapsskjemaErOriginalt;

  @XmlElement private Boolsk manuellBehandlingErOensket;

  @XmlElement private Tekst kommentarForManuellBehandling;
}
