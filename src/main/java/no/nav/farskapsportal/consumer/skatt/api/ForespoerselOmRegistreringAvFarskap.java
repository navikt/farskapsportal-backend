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
@XmlType(propOrder = {"registreringsdato", "avsendersSaksreferanse", "innsender", "mottak", "saksbehandlersVurdering", "vedlegg",
    "barn", "far", "mor", "foreldreBorSammen"})
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
  private Vedlegg vedlegg;

  @XmlElement
  private Barn barn;

  @XmlElement
  private Far far;

  @XmlElement
  private Mor mor;

  @XmlElement
  private Boolsk foreldreBorSammen;

}
