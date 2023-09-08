package no.nav.farskapsportal.backend.libs.dto.skatt.api;

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
@XmlRootElement(namespace = "folkeregisteret:melding:farskap:v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"innsending", "forespoersel"})
public class MeldingOmRegistreringAvFarskap {

  @XmlElement private Innsending innsending;

  @XmlElement private ForespoerselOmRegistreringAvFarskap forespoersel;
}
