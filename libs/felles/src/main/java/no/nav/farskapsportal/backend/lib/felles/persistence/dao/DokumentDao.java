package no.nav.farskapsportal.backend.lib.felles.persistence.dao;

import no.nav.farskapsportal.backend.lib.entity.Dokument;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DokumentDao extends CrudRepository<Dokument, Integer> {}
