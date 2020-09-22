package no.nav.farskapsportal.consumer.pdl;

import no.nav.farskapsportal.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSubQuery;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static no.nav.farskapsportal.FarskapsportalApiApplicationLocal.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PdlApiConsumer")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(
        classes = {FarskapsportalApiApplicationLocal.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 8096)
public class PdlApiConsumerTest {

    @Autowired
    private PdlApiConsumer pdlApiConsumer;

    @Autowired
    private PdlApiStub pdlApiStub;

    @Autowired
    private StsStub stsStub;


    @Test
    @DisplayName("Skal hente kjønn hvis person eksisterer")
    public void skalHenteKjoennHvisPersonEksisterer() {
        var fnrMor = "111222240280";
        var kjoennMor = Kjoenn.KVINNE;

        stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");

        List<HentPersonSubQuery> subQueries = List.of(new HentPersonKjoenn(Kjoenn.KVINNE));

        pdlApiStub.runPdlApiHentPersonStub(subQueries);

        var respons = pdlApiConsumer.henteKjoenn(fnrMor);

        var returnertKjoenn = respons.getResponseEntity().getBody();

        assertAll(
                () -> assertThat(respons.is2xxSuccessful()),
                () -> assertTrue(kjoennMor.toString().equals(returnertKjoenn.name()))
        );
    }

    @Test
    @DisplayName("Skal feile dersom informasjon om kjønn mangler")
    public void skalGi404DersomInformasjonOmKjoennMangler() {
        var fnrMor = "111222240280";

        stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");

        List<HentPersonSubQuery> subQueries = List.of(new HentPersonKjoenn(null));

        pdlApiStub.runPdlApiHentPersonStub(subQueries);

        var respons = pdlApiConsumer.henteKjoenn(fnrMor);

        var returnertKjoenn = respons.getResponseEntity().getBody();

        assertAll(
                () -> assertTrue(HttpStatus.NOT_FOUND.equals(respons.getResponseEntity().getStatusCode())),
                () -> assertTrue(returnertKjoenn == null));
    }
}
