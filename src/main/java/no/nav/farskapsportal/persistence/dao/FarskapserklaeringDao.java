package no.nav.farskapsportal.persistence.dao;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FarskapserklaeringDao extends CrudRepository<Farskapserklaering, Integer> {

  @Query("select fe from Farskapserklaering fe where (fe.far.foedselsnummer = :fnr or fe.mor.foedselsnummer = :fnr) and fe.dokument.padesUrl is not null")
  Set<Farskapserklaering> hentFarskapserklaeringerMedPadeslenke(String fnr);

  @Query("select fe from Farskapserklaering fe where fe.far.foedselsnummer =:fnrFar and fe.mor.foedselsnummer =:fnrMor and fe.barn.termindato = :termindato and fe.dokument.padesUrl is not null")
  Set<Farskapserklaering> hentFarskapserklaeringMedPadeslenke(String fnrFar, String fnrMor, Date termindato);

  @Query("select fe from Farskapserklaering fe where (fe.far.foedselsnummer =:fnrForelder or fe.mor.foedselsnummer = :fnrForelder)  and fe.barn.foedselsnummer = :fnrBarn and fe.dokument.padesUrl is not null")
  Farskapserklaering hentFarskapserklaeringMedPadeslenke(String fnrForelder, String fnrBarn);

  @Query("select fe from Farskapserklaering  fe where fe.mor.foedselsnummer = :fnrMor and fe.dokument.padesUrl is null")
 Set<Farskapserklaering> hentFarskapserklaeringerMorUtenPadeslenke(String fnrMor);
}
