package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import org.springframework.data.repository.CrudRepository;

public interface DokumentinnholdDao extends CrudRepository<Dokumentinnhold, Integer> {

}
