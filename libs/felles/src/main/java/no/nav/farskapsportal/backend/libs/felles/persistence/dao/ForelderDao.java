package no.nav.farskapsportal.backend.libs.felles.persistence.dao;

import java.util.Optional;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForelderDao extends CrudRepository<Forelder, Integer> {

  @Query("select f from Forelder  f where f.foedselsnummer = :fnrForelder")
  Optional<Forelder> henteForelderMedFnr(String fnrForelder);
}
