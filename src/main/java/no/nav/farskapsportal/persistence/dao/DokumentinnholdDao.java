package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DokumentinnholdDao extends CrudRepository<Dokumentinnhold, Integer> {

}
