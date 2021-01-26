package no.nav.farskapsportal.persistence.dao;

import java.util.Optional;
import no.nav.farskapsportal.persistence.entity.Forelder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ForelderDao extends CrudRepository<Forelder, Integer> {

  @Query("select f from Forelder  f where f.foedselsnummer = :fnrForelder")
  Optional<Forelder> henteForelderMedFnr(String fnrForelder);
}
