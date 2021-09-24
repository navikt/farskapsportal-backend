package no.nav.farskapsportal.backend.libs.felles.persistence.dao;

import no.nav.farskapsportal.backend.libs.entity.Meldingslogg;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeldingsloggDao extends CrudRepository<Meldingslogg, Integer> {

}
