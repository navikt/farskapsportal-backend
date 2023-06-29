package no.nav.farskapsportal.backend.libs.dto.skatt.api;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "forespoersel")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"registreringsdato", "avsendersSaksreferanse", "innsender", "mottak", "saksbehandlersVurdering", "vedlegg", "barnet", "far",
    "mor", "foreldreBorSammen"})
public class ForespoerselOmRegistreringAvFarskap {

  @XmlElement
  private Dato registreringsdato;

  @XmlElement
  private Tekst avsendersSaksreferanse;

  @XmlElement
  private Innsender innsender;

  @XmlElement
  private Informasjonsmottak mottak;

  @XmlElement
  private SaksbehandlersVurdering saksbehandlersVurdering;

  @XmlElement
  private List<Vedlegg> vedlegg;

  @XmlElement
  private Barn barnet;

  @XmlElement
  private Far far;

  @XmlElement
  private Mor mor;

  @XmlElement
  private Boolsk foreldreBorSammen;

}
