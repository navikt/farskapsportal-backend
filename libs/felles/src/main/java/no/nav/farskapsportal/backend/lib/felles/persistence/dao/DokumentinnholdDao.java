package no.nav.farskapsportal.backend.lib.felles.persistence.dao;

import no.nav.farskapsportal.backend.lib.entity.Dokumentinnhold;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DokumentinnholdDao extends CrudRepository<Dokumentinnhold, Integer> {

}
