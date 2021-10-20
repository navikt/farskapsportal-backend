package no.nav.farskapsportal.persistence.dao;

import java.util.Optional;
import java.util.Set;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OppgavebestillingDao extends CrudRepository<Oppgavebestilling, Integer> {

  @Query("select o from Oppgavebestilling o where o.farskapserklaering.id = :idFarskapserklaering and o.forelder.foedselsnummer = :fnr and o.eventId is not null and o.ferdigstilt is null")
  Set<Oppgavebestilling> henteAktiveOppgaver(int idFarskapserklaering, String fnr);

  @Query("select o from Oppgavebestilling o where o.eventId = :eventId")
  Optional<Oppgavebestilling> henteOppgavebestilling(String eventId);

}
