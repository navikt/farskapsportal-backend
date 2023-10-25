package no.nav.farskapsportal.backend.libs.felles.persistence.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarskapserklaeringDao extends CrudRepository<Farskapserklaering, Integer> {

  @Query(
      "select fe from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and (fe.far.foedselsnummer = :fnrForelder or fe.mor.foedselsnummer = :fnrForelder)")
  Set<Farskapserklaering> henteFarskapserklaeringerForForelder(String fnrForelder);

  @Query(
      "select fe from Farskapserklaering fe where fe.deaktivert is null and fe.mor.foedselsnummer =:fnrMor")
  Set<Farskapserklaering> henteMorsErklaeringer(String fnrMor);

  @Query(
      "select fe from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and fe.far.foedselsnummer =:fnrFar "
          + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null")
  Set<Farskapserklaering> henteFarsErklaeringer(String fnrFar);

  @Query(
      "select fe from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and fe.barn.foedselsnummer =:fnrBarn")
  Set<Farskapserklaering> henteBarnsErklaeringer(String fnrBarn);

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is not null "
          + "and fe.sendtTilSkatt is null "
          + " order by fe.dokument.signeringsinformasjonFar.signeringstidspunkt desc")
  Set<Integer> henteFarskapserklaeringerErKlareForOverfoeringTilSkatt();

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null "
          + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt < :sisteGyldigeDagForIkkeFerdigstiltSigneringsoppdrag "
          + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is null")
  Set<Integer> henteIdTilAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag(
      LocalDateTime sisteGyldigeDagForIkkeFerdigstiltSigneringsoppdrag);

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and fe.sendtTilSkatt is not null "
          + "and fe.sendtTilSkatt < :sendtTilSkattFoer "
          + "and (fe.barn.termindato is null or fe.barn.termindato < :termindatoFoer)")
  Set<Integer> henteIdTilOversendteFarskapserklaeringerSomSkalDeaktiveres(
      LocalDateTime sendtTilSkattFoer, LocalDate termindatoFoer);

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and fe.farBorSammenMedMor is not null "
          + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is not null "
          + "and fe.dokument.signeringsinformasjonFar.sendtTilSignering < :farSendtTilSigneringFoer "
          + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is null")
  Set<Integer> henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoForFar(
      LocalDateTime farSendtTilSigneringFoer);

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.deaktivert is null "
          + "and fe.dokument.signeringsinformasjonMor.signeringstidspunkt is null "
          + "and fe.dokument.signeringsinformasjonMor.sendtTilSignering is not null "
          + "and fe.dokument.signeringsinformasjonMor.sendtTilSignering < :morSendtTilSigneringFoer "
          + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt is null")
  Set<Integer> henteIdTilFarskapserklaeringerSomManglerMorsSignatur(
      LocalDateTime morSendtTilSigneringFoer);

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.sendtTilSkatt is not null "
          + "and fe.oppgaveSendt is null "
          + "and fe.farBorSammenMedMor = false "
          + "and (fe.barn.foedselsnummer is not null or (fe.barn.termindato is not null and fe.barn.termindato < :grenseTermindato)) "
          + "and fe.dokument.signeringsinformasjonFar.signeringstidspunkt < :grensetidspunktSignering")
  Set<Integer> henteIdTilFarskapserklaeringerDetSkalOpprettesOppgaverFor(
      LocalDate grenseTermindato, LocalDateTime grensetidspunktSignering);

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.deaktivert is not null "
          + "and fe.deaktivert < :deaktivertFoer "
          + "and (fe.dokument.dokumentinnhold.innhold != null or fe.dokument.signeringsinformasjonFar.xadesXml != null or fe.dokument.signeringsinformasjonMor.xadesXml != null "
          + " or fe.dokument.blobIdGcp != null or fe.dokument.signeringsinformasjonFar.blobIdGcp != null or fe.dokument.signeringsinformasjonMor.blobIdGcp != null) "
          + "order by fe.dokument.signeringsinformasjonFar.signeringstidspunkt asc")
  Set<Integer> henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
      LocalDateTime sendtTilSkattFoer, LocalDateTime deaktivertFoer);

  @Query(
      "select fe.id from Farskapserklaering fe "
          + "where fe.sendtTilSkatt is not null "
          + "and fe.sendtTilSkatt < :sendtTilSkattFoer "
          + "and fe.deaktivert is not null "
          + "and fe.deaktivert < :deaktivertFoer "
          + "and (fe.dokument.dokumentinnhold.innhold != null or fe.dokument.signeringsinformasjonFar.xadesXml != null or fe.dokument.signeringsinformasjonMor.xadesXml != null) "
          + "and fe.dokument.blobIdGcp is null and fe.dokument.signeringsinformasjonFar.blobIdGcp is null and fe.dokument.signeringsinformasjonMor.blobIdGcp is null "
          + "order by fe.dokument.signeringsinformasjonFar.signeringstidspunkt asc")
  Set<Integer> henteIdTilFarskapserklaeringerSomSkalMigreresTilBuckets(
      LocalDateTime sendtTilSkattFoer, LocalDateTime deaktivertFoer);
}
