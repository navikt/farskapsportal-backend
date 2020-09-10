package no.nav.farskapsportal.api.graphql.queries.person;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import no.nav.pdl.api.graphql.queries.person.typer.AdressebeskyttelseDto;
import no.nav.pdl.api.graphql.queries.person.typer.BostedsadresseDto;
import no.nav.pdl.api.graphql.queries.person.typer.DeltBostedDto;
import no.nav.pdl.api.graphql.queries.person.typer.DoedfoedtBarnDto;
import no.nav.pdl.api.graphql.queries.person.typer.DoedsfallDto;
import no.nav.pdl.api.graphql.queries.person.typer.FalskIdentitetDto;
import no.nav.pdl.api.graphql.queries.person.typer.FamilierelasjonDto;
import no.nav.pdl.api.graphql.queries.person.typer.FoedselDto;
import no.nav.pdl.api.graphql.queries.person.typer.FolkeregisteridentifikatorDto;
import no.nav.pdl.api.graphql.queries.person.typer.FolkeregisterpersonstatusDto;
import no.nav.pdl.api.graphql.queries.person.typer.ForeldreansvarDto;
import no.nav.pdl.api.graphql.queries.person.typer.FullmaktDto;
import no.nav.pdl.api.graphql.queries.person.typer.GeografiskTilknytningDto;
import no.nav.pdl.api.graphql.queries.person.typer.IdentitetsgrunnlagDto;
import no.nav.pdl.api.graphql.queries.person.typer.InnflyttingTilNorgeDto;
import no.nav.pdl.api.graphql.queries.person.typer.KjoennDto;
import no.nav.pdl.api.graphql.queries.person.typer.KontaktadresseDto;
import no.nav.pdl.api.graphql.queries.person.typer.KontaktinformasjonForDoedsboDto;
import no.nav.pdl.api.graphql.queries.person.typer.NavnDto;
import no.nav.pdl.api.graphql.queries.person.typer.OppholdDto;
import no.nav.pdl.api.graphql.queries.person.typer.OppholdsadresseDto;
import no.nav.pdl.api.graphql.queries.person.typer.SikkerhetstiltakDto;
import no.nav.pdl.api.graphql.queries.person.typer.SivilstandDto;
import no.nav.pdl.api.graphql.queries.person.typer.StatsborgerskapDto;
import no.nav.pdl.api.graphql.queries.person.typer.TelefonnummerDto;
import no.nav.pdl.api.graphql.queries.person.typer.TilrettelagtKommunikasjonDto;
import no.nav.pdl.api.graphql.queries.person.typer.UtenlandskIdentifikasjonsnummerDto;
import no.nav.pdl.api.graphql.queries.person.typer.UtflyttingFraNorgeDto;
import no.nav.pdl.api.graphql.queries.person.typer.VergemaalEllerFremtidsfullmaktDto;

import java.util.List;

@Value
@Builder
@SuppressWarnings("pmd:TooManyFields")
public class PersonDto {

    @Singular("adressebeskyttelse")
    List<AdressebeskyttelseDto> adressebeskyttelse;

    @Singular("bostedsadresse")
    List<BostedsadresseDto> bostedsadresse;

    @Singular("deltBosted")
    List<DeltBostedDto> deltBosted;

    @Singular("doedsfall")
    List<DoedsfallDto> doedsfall;

    @Singular("doedfoedtBarn")
    List<DoedfoedtBarnDto> doedfoedtBarn;

    FalskIdentitetDto falskIdentitet;

    @Singular("familierelasjon")
    List<FamilierelasjonDto> familierelasjoner;

    @Singular("foedsel")
    List<FoedselDto> foedsel;

    @Singular("folkeregisteridentifikator")
    List<FolkeregisteridentifikatorDto> folkeregisteridentifikator;

    @Singular("folkeregisterpersonstatus")
    List<FolkeregisterpersonstatusDto> folkeregisterpersonstatus;

    @Singular("foreldreansvar")
    List<ForeldreansvarDto> foreldreansvar;

    @Singular("fullmakt")
    List<FullmaktDto> fullmakt;

    @Singular("identitetsgrunnlag")
    List<IdentitetsgrunnlagDto> identitetsgrunnlag;

    @Singular("kontaktadresse")
    List<KontaktadresseDto> kontaktadresse;

    @Singular("kontaktinformasjonForDoedsbo")
    List<KontaktinformasjonForDoedsboDto> kontaktinformasjonForDoedsbo;

    @Singular("kjoenn")
    List<KjoennDto> kjoenn;

    @Singular("navn")
    List<NavnDto> navn;

    @Singular("opphold")
    List<OppholdDto> opphold;

    @Singular("oppholdsadresse")
    List<OppholdsadresseDto> oppholdsadresse;

    @Singular("sikkerhetstiltak")
    List<SikkerhetstiltakDto> sikkerhetstiltak;

    @Singular("sivilstand")
    List<SivilstandDto> sivilstand;

    @Singular("statsborgerskap")
    List<StatsborgerskapDto> statsborgerskap;

    @Singular("tilrettelagtKommunikasjon")
    List<TilrettelagtKommunikasjonDto> tilrettelagtKommunikasjon;

    @Singular("utenlandskIdentifikasjonsnummer")
    List<UtenlandskIdentifikasjonsnummerDto> utenlandskIdentifikasjonsnummer;

    @Singular("telefonnummer")
    List<TelefonnummerDto> telefonnummer;

    @Singular("innflyttingTilNorge")
    List<InnflyttingTilNorgeDto> innflyttingTilNorge;

    @Singular("utflyttingFraNorge")
    List<UtflyttingFraNorgeDto> utflyttingFraNorge;

    @Singular("vergemaalEllerFremtidsfullmakt")
    List<VergemaalEllerFremtidsfullmaktDto> vergemaalEllerFremtidsfullmakt;

    GeografiskTilknytningDto geografiskTilknytning;
}