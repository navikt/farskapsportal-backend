package no.nav.farskapsportal.controller;

import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.farskapsportal.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerResponse;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static no.nav.farskapsportal.FarskapsportalApiApplicationLocal.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
@DisplayName("FarskapsportalController")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApiApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 8096)
public class FarskapsportalControllerTest {

    @LocalServerPort
    private int localServerPort;

    @Autowired
    private HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;

    @Autowired
    private StsStub stsStub;

    @Autowired
    private PdlApiStub pdlApiStub;

    @Test
    @DisplayName("Skal finne kjønn til person")
    void skalFinneKjoennTilPerson() {
        var foedselsnummerMor = "01018912345";

        stsStub.runSecurityTokenServiceStub("jalla");
        pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonKjoenn(Kjoenn.KVINNE)));

        var respons = httpHeaderTestRestTemplate.exchange(initHenteKjoennUrl(foedselsnummerMor), HttpMethod.GET, null, KontrollerePersonopplysningerResponse.class);

        assertAll(
                () -> assertThat(HttpStatus.OK.equals(respons.getStatusCode())),
                () -> assertThat(respons.getBody().isRiktigKombinasjonAvFodselnummerOgNavn())
        );

    }

    private String initHenteKjoennUrl(String foedselsnummer) {
        return getBaseUrlForStubs() + "/api/v1/farskapsportal/kjoenn/" + foedselsnummer;
    }

    private String getBaseUrlForStubs() {
        return "http://localhost:" + localServerPort;
    }

}
