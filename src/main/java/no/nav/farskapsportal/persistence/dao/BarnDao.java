package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Barn;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarnDao extends CrudRepository<Barn, Integer> {

}
