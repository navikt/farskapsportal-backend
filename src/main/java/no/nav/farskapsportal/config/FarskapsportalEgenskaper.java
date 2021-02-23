package no.nav.farskapsportal.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class FarskapsportalEgenskaper {

  @Value("${farskapsportal.egenskaper.min-antall-uker-til-termindato}")
  private int minAntallUkerTilTermindato;
  @Value("${farskapsportal.egenskaper.maks-antall-uker-til-termindato}")
  private int maksAntallUkerTilTermindato;
  @Value("${farskapsportal.egenskaper.maks-antall-maaneder-etter-foedsel}")
  private int maksAntallMaanederEtterFoedsel;
  @Value("${farskapsportal.egenskaper.nav.orgnummer}")
  private String orgnummerNav;
  @Value("${ESIGNERING_FULLFOERT_URL}")
  private String esigneringFullfoertUrl;
  @Value("${ESIGNERING_ABRUTT_URL}")
  private String esigneringAvbruttUrl;
  @Value("${ESIGNERING_FEILET_URL}")
  private String esigneringFeiletUrl;

}
