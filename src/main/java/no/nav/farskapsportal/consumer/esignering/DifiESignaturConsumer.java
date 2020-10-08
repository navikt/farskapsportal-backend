package no.nav.farskapsportal.consumer.esignering;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.ForelderRolle;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.ForelderDto;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DifiESignaturConsumer {

  public void signereDokument(DokumentDto dokument, ForelderDto forelderDto) {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    log.info("Signerer farskapserklæring for " + forelderDto.getForelderRolle().toString());

    try {
      outputStream.write(dokument.getDokument());
      outputStream.write(forelderDto.getFoedselsnummer().getBytes());
    } catch (IOException e) {
      throw new ESigneringFeiletException("Feil ved signering av dokument for " + forelderDto.getForelderRolle().toString());
    }

    dokument.setDokument(outputStream.toByteArray());

    if (forelderDto.getForelderRolle().equals(ForelderRolle.MOR)) {
      dokument.setSignertAvMor(true);
    } else {
      dokument.setSignertAvFar(true);
    }
  }
}
