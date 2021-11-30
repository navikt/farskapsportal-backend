package no.nav.farskapsportal.backend.libs.felles.persistence.dao;

import java.time.LocalDateTime;
import java.util.Set;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarskapserklaeringDao extends CrudRepository<Farskapserklaering, Integer> {

  @Query(
      "select fe from Farskapserklaering fe where (fe.far.foedselsnummer = :fnr or fe.mor.foedselsnummer = :fnr) "
          + "and (fe.dokument.padesUrl is not null or fe.dokument.padesUrl is not null) and fe.deaktivert is null")
  Set<Farskapserklaering> hentFarskapserklaeringerMedPadeslenke(String fnr);

  @Query("select fe from Farskapserklaering fe where (fe.far.foedselsnummer = :fnrForelder or fe.mor.foedselsnummer = :fnrForelder) and fe.deaktivert is null")
  Set<Farskapserklaering> henteFarskapserklaeringerForForelder(String fnrForelder);

  @Query(
      "select fe from Farskapserklaering fe where fe.mor.foedselsnummer =:fnrMor and fe.deaktivert is null")
  Set<Farskapserklaering> henteMorsErklaeringer(String fnrMor);

  @Query(
      "select fe from Farskapserklaering fe where fe.far.foedselsnummer =:fnrFar and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null and fe.deaktivert is null")
  Set<Farskapserklaering> henteFarsErklaeringer(String fnrFar);

  @Query(
      "select fe from Farskapserklaering fe where fe.barn.foedselsnummer =:fnrBarn and fe.deaktivert is null")
  Set<Farskapserklaering> henteBarnsErklaeringer(String fnrBarn);

  @Query(
      "select fe from Farskapserklaering  fe where fe.mor.foedselsnummer =:fnrMor and fe.dokument.padesUrl is null and fe.deaktivert is null")
  Set<Farskapserklaering> hentFarskapserklaeringerMorUtenPadeslenke(String fnrMor);

  @Query("select fe from Farskapserklaering fe where fe.dokument.signeringsinformasjonFar.signeringstidspunkt is not null "
      + "and fe.sendtTilSkatt is not null and fe.farBorSammenMedMor = false and fe.sendtTilJoark is null and fe.deaktivert is null")
  Set<Farskapserklaering> henteFarskapserklaeringerSomTidligereErForsoektSendtTilJoark();

  @Query("select fe.id from Farskapserklaering fe where fe.dokument.signeringsinformasjonFar.signeringstidspunkt is not null"
      + " and fe.sendtTilSkatt is null and fe.deaktivert is null")
  Set<Integer> henteFarskapserklaeringerErKlareForOverfoeringTilSkatt();

  @Query("select fe.id from Farskapserklaering fe where fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null "
      + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is null and fe.deaktivert is null")
  Set<Integer> henteIdTilFarskapserklaeringerSomVenterPaaFarsSignatur();

  @Query("select fe.id from Farskapserklaering fe "
      + "where fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null "
      + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt < :sisteGyldigeDagForIkkeFerdigstiltSigneringsoppdrag "
      + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is null "
      + "and fe.deaktivert is null")
  Set<Integer> henteIdTilAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag(
      LocalDateTime sisteGyldigeDagForIkkeFerdigstiltSigneringsoppdrag);

  @Query("select fe.id from Farskapserklaering fe "
      + "where fe.farBorSammenMedMor is not null "
      + "and fe.deaktivert is null "
      + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null "
      + "and fe.dokument.signeringsinformasjonFar.sendtTilSignering < :farSendtTilSigneringFoer "
      + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is null")
  Set<Integer> henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoForFar(LocalDateTime farSendtTilSigneringFoer);

  @Query("select fe.id from Farskapserklaering fe "
      + "where fe.farBorSammenMedMor is not null "
      + "and fe.deaktivert is null "
      + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null "
      + "and fe.dokument.signeringsinformasjonFar.sendtTilSignering is null "
      + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is null")
  @Deprecated
  // TODO: (30.11.2021) Kan fjernes når alle aktuelle farskapserklæringer har sendtTilSignering satt for far
  Set<Integer> henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoForFar();
}
