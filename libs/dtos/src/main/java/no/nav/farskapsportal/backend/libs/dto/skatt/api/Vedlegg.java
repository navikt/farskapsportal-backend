package no.nav.farskapsportal.backend.libs.dto.skatt.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class Vedlegg {

  private Tekst dokumenttype;

  private Tekst multipartnavn;
}
