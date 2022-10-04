package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.BostedsadresseDto;

@Value
@Getter
public class HentPersonBostedsadresse implements HentPersonSubResponse {

  String response;

  public HentPersonBostedsadresse(BostedsadresseDto bostedsadresseDto) {
    this.response = buildResponse(bostedsadresseDto);
  }

  private String buildResponse(BostedsadresseDto bostedsadresseDto) {

    if (bostedsadresseDto.getVegadresse() != null) {

      return String.join(
          "\n",
          " \"bostedsadresse\": [",
          " {",
          " \"vegadresse\": {",
          " \"adressenavn\": \"" + bostedsadresseDto.getVegadresse().getAdressenavn() + "\",",
          " \"husnummer\": \"" + bostedsadresseDto.getVegadresse().getHusnummer() + "\",",
          " \"husbokstav\": \"" + bostedsadresseDto.getVegadresse().getHusbokstav() + "\",",
          " \"postnummer\": \"" + bostedsadresseDto.getVegadresse().getPostnummer() + "\"",
          " },",
          " \"metadata\": {",
          " \"master\": \"FREG\"",
          " }",
          " }",
          "]");
    } else {
      return String.join(
          "\n",
          " \"bostedsadresse\": [",
          " {",
          " \"utenlandsadresse\": {",
          " \"adressenavnNummer\": \"" + bostedsadresseDto.getUtenlandskAdresse().getAdressenavnNummer() + "\"",
          " },",
          " \"metadata\": {",
          " \"master\": \"FREG\"",
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
