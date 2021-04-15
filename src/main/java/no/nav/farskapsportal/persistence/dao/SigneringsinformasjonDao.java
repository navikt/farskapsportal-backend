package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Signeringsinformasjon;
import org.springframework.data.repository.CrudRepository;

public interface SigneringsinformasjonDao extends CrudRepository<Signeringsinformasjon, Integer> {

}
