package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Dokument;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DokumentDao extends CrudRepository<Dokument, Integer> {}
