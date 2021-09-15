package no.nav.farskapsportal.consumer.pdl.stub;

import lombok.Getter;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
public class HentPersonNavn implements HentPersonSubResponse {

  private ModelMapper modelMapper;

  private String response;


  public HentPersonNavn(no.nav.farskapsportal.dto.NavnDto navn) {
    this.modelMapper = new ModelMapper();
    var pdlNavnDto = modelMapper.map(navn, NavnDto.class);
    buildResponse(pdlNavnDto, "sagga-dagga");
  }

  private void buildResponse(NavnDto navnDto, String opplysningsId) {
    if (navnDto == null) {
      this.response = String.join("\n", " \"navn\": [", "]");
    } else {

      var fornavn = navnDto.getFornavn() == null ? null : "\"" + navnDto.getFornavn() + "\"";
      var mellomnavn =
          navnDto.getMellomnavn() == null ? null : "\"" + navnDto.getMellomnavn() + "\"";
      var etternavn = navnDto.getEtternavn() == null ? null : "\"" + navnDto.getEtternavn() + "\"";

      this.response =
          String.join(
              "\n",
              " \"navn\": [",
              " {",
              " \"fornavn\": " + fornavn + ",",
              " \"mellomnavn\":" + mellomnavn + ",",
              " \"etternavn\":" + etternavn + ",",
              " \"metadata\": {",
              " \"opplysningsId\": \"" + opplysningsId + "\",",
              " \"master\": \"Freg\"",
              " }",
              " }",
              "]");
    }
  }

  @Override
  public String hentRespons(boolean medHistorikk) {
    return medHistorikk ? response : response;
  }
}
