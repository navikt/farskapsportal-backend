package no.nav.farskapsportal.backend.libs.felles.persistence.dao;

import java.util.Optional;
import no.nav.farskapsportal.backend.libs.entity.StatusKontrollereFar;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusKontrollereFarDao extends CrudRepository<StatusKontrollereFar, Integer> {

  @Query("select skf from StatusKontrollereFar skf where skf.mor.foedselsnummer = :fnrMor")
  Optional<StatusKontrollereFar> henteStatusKontrollereFar(String fnrMor);
}
