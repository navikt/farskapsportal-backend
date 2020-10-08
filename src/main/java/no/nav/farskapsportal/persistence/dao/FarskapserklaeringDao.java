package no.nav.farskapsportal.persistence.dao;

import java.time.LocalDate;
import java.util.Set;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FarskapserklaeringDao extends CrudRepository<Farskapserklaering, Integer> {

  @Query("select fe from Farskapserklaering fe where fe.far.foedselsnummer = :fnrFar")
  Set<Farskapserklaering> hentFarskapserklaeringerFar(String fnrFar);

  @Query("select fe from Farskapserklaering fe where fe.far.foedselsnummer =:fnrFar and fe.barn.foedselsnummer = :fnrBarn")
  Farskapserklaering hentFarskapserklaeringFar(String fnrFar, String fnrBarn);

  @Query("select fe from Farskapserklaering fe where fe.far.foedselsnummer =:fnrFar and fe.barn.termindato = :termindato")
  Farskapserklaering hentFarskapserklaeringFar(String fnrFar, LocalDate termindato);

  @Query("select fe from Farskapserklaering fe where fe.mor.foedselsnummer = :fnrMor")
  Set<Farskapserklaering> hentFarskapserklaeringerMor(String fnrMor);

  @Query("select fe from Farskapserklaering fe where fe.mor.foedselsnummer = :fnrMor and fe.barn.foedselsnummer = :fnrBarn")
  Farskapserklaering hentFarskapserklaeringMor(String fnrMor, String fnrBarn);

  @Query("select fe from Farskapserklaering fe where fe.mor.foedselsnummer = :fnrMor and fe.barn.foedselsnummer = :termindato")
  Farskapserklaering hentFarskapserklaeringMor(String fnrMor, LocalDate termindatos);
}
