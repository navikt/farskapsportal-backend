package no.nav.farskapsportal.backend.asynkron.consumer.skatt.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
