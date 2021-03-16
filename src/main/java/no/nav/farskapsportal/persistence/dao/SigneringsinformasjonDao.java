package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.data.repository.CrudRepository;

public interface SigneringsinformasjonDao extends CrudRepository<Farskapserklaering, Integer> {

}
