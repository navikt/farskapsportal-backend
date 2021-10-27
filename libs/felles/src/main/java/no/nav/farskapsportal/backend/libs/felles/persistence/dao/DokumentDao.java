package no.nav.farskapsportal.backend.libs.felles.persistence.dao;

import no.nav.farskapsportal.backend.libs.entity.Dokument;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DokumentDao extends CrudRepository<Dokument, Integer> {}
