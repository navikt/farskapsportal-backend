package no.nav.farskapsportal.backend.lib.felles.persistence.dao;

import no.nav.farskapsportal.backend.lib.entity.Signeringsinformasjon;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SigneringsinformasjonDao extends CrudRepository<Signeringsinformasjon, Integer> {

}
