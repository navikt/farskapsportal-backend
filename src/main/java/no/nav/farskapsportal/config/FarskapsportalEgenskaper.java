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
  @Value("${farskapsportal.egenskaper.kontroll.far.maks-antall-forsoek}")
  private int maksAntallForsoek;
  @Value("${farskapsportal.egenskaper.kontroll.far.forsoek-fornyes-etter-antall-dager}")
  private int forsoekFornyesEtterAntallDager;
  @Value("${farskapsportal.egenskaper.nav.orgnummer}")
  private String orgnummerNav;
  @Value("${farskapsportal.egenskaper.posten.esignering.suksess-url}")
  private String esigneringSuksessUrl;
  @Value("${farskapsportal.egenskaper.posten.esignering.avbrutt-url}")
  private String esigneringAvbruttUrl;
  @Value("${farskapsportal.egenskaper.posten.esignering.feilet-url}")
  private String esigneringFeiletUrl;
}
