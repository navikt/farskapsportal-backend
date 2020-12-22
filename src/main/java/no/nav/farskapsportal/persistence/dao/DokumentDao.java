package no.nav.farskapsportal.persistence.dao;

import no.nav.farskapsportal.persistence.entity.Dokument;
import org.springframework.data.repository.CrudRepository;

public interface DokumentDao extends CrudRepository<Dokument, Integer> {}
