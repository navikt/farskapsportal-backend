package no.nav.farskapsportal.backend.lib.felles.persistence.dao;

import java.util.Optional;
import no.nav.farskapsportal.backend.lib.entity.StatusKontrollereFar;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusKontrollereFarDao extends CrudRepository<StatusKontrollereFar, Integer> {

  @Query("select skf from StatusKontrollereFar skf where skf.mor.foedselsnummer = :fnrMor")
  Optional<StatusKontrollereFar> henteStatusKontrollereFar(String fnrMor);

}
