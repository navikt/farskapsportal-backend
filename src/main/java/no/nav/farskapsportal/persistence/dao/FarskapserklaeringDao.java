package no.nav.farskapsportal.persistence.dao;

import java.time.LocalDate;
import java.util.Set;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FarskapserklaeringDao extends CrudRepository<Farskapserklaering, Integer> {

  @Query(
      "select fe from Farskapserklaering fe where (fe.far.foedselsnummer = :fnr or fe.mor.foedselsnummer = :fnr) and (fe.dokument.padesUrl is not null or fe.dokument.padesUrl is not null )")
  Set<Farskapserklaering> hentFarskapserklaeringerMedPadeslenke(String fnr);

  @Query("select fe from Farskapserklaering fe where fe.far.foedselsnummer = :fnrFar")
  Set<Farskapserklaering> henteFarskapserklaeringerForFar(String fnrFar);

  @Query("select fe from Farskapserklaering fe where fe.far.foedselsnummer = :fnrForelder or fe.mor.foedselsnummer = :fnrForelder")
  Set<Farskapserklaering> henteFarskapserklaeringerForForelder(String fnrForelder);

  @Query(
      "select fe from Farskapserklaering fe where fe.mor.foedselsnummer = :fnrMor and fe.far.foedselsnummer = :fnrFar "
          + "and  fe.barn.termindato > :nedreGrenseTermindato and fe.barn.termindato <= :oevreGrenseTermindato")
  Set<Farskapserklaering> henteFarskapserklaeringerForForelder(
      String fnrMor, String fnrFar, LocalDate nedreGrenseTermindato, LocalDate oevreGrenseTermindato);

  @Query(
      "select fe from Farskapserklaering fe where fe.mor.foedselsnummer =:fnrMor")
  Set<Farskapserklaering> henteMorsErklaeringer(String fnrMor);

  @Query(
      "select fe from Farskapserklaering fe where fe.far.foedselsnummer =:fnrFar "
          + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null")
  Set<Farskapserklaering> henteFarsErklaeringer(String fnrFar);

  @Query(
      "select fe from Farskapserklaering fe where fe.barn.foedselsnummer =:fnrBarn")
  Set<Farskapserklaering> henteBarnsErklaeringer(String fnrBarn);

  @Query(
      "select fe from Farskapserklaering  fe where fe.mor.foedselsnummer =:fnrMor and fe.dokument.padesUrl is null")
  Set<Farskapserklaering> hentFarskapserklaeringerMorUtenPadeslenke(String fnrMor);

  @Query("select fe from Farskapserklaering fe where fe.dokument.signeringsinformasjonFar.signeringstidspunkt is not null "
      + "and fe.meldingsidSkatt is not null and fe.sendtTilSkatt is null")
  Set<Farskapserklaering> henteFarskapserklaeringerErKlareForOverfoeringTilSkatt();
}
