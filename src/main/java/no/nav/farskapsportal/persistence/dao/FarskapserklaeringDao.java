package no.nav.farskapsportal.persistence.dao;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FarskapserklaeringDao extends CrudRepository<Farskapserklaering, Integer> {

  @Query(
      "select fe from Farskapserklaering fe where (fe.far.foedselsnummer = :fnr or fe.mor.foedselsnummer = :fnr) and fe.dokument.padesUrl is not null")
  Set<Farskapserklaering> hentFarskapserklaeringerMedPadeslenke(String fnr);

  @Query(
      "select fe from Farskapserklaering fe where  fe.mor.foedselsnummer = :fnrMor and fe.far.foedselsnummer = :fnrFar and fe.barn.foedselsnummer = :fnrBarn")
  Optional<Farskapserklaering> henteUnikFarskapserklaering(String fnrMor, String fnrFar, String fnrBarn);

  @Query(
      "select fe from Farskapserklaering fe where fe.mor.foedselsnummer = :fnrMor and fe.far.foedselsnummer = :fnrFar and  fe.barn.termindato = :termindato")
  Optional<Farskapserklaering> henteUnikFarskapserklaering(
      String fnrMor, String fnrFar, LocalDate termindato);

  @Query(
      "select fe from Farskapserklaering fe where fe.mor.foedselsnummer = :fnrMor and fe.far.foedselsnummer = :fnrFar "
          + "and  fe.barn.termindato > :nedreGrenseTermindato and fe.barn.termindato <= :oevreGrenseTermindato")
  Set<Farskapserklaering> henteFarskapserklaeringer(
      String fnrMor, String fnrFar, LocalDate nedreGrenseTermindato, LocalDate oevreGrenseTermindato);

  @Query(
      "select fe from Farskapserklaering fe where fe.mor.foedselsnummer = :fnrMor")
  Set<Farskapserklaering> henteMorsErklaeringer(String fnrMor);

  @Query(
      "select fe from Farskapserklaering fe where fe.far.foedselsnummer =:fnrFar and fe.dokument.signertAvMor is not null")
  Set<Farskapserklaering> henteFarsErklaeringer(String fnrFar);

  @Query(
      "select fe from Farskapserklaering  fe where fe.mor.foedselsnummer = :fnrMor and fe.dokument.padesUrl is null")
  Set<Farskapserklaering> hentFarskapserklaeringerMorUtenPadeslenke(String fnrMor);
}
