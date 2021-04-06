package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Meldingslogg;
import org.springframework.data.repository.CrudRepository;

public interface MeldingsloggDao extends CrudRepository<Meldingslogg, Integer> {

}
