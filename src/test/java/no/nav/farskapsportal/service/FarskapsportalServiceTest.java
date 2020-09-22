package no.nav.farskapsportal.service;

import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static no.nav.farskapsportal.FarskapsportalApiApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DisplayName("FarskapsportalService")
@SpringBootTest(classes = FarskapsportalApiApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class FarskapsportalServiceTest {


    @MockBean
    private PdlApiConsumer pdlApiConsumerMock;

    @Autowired
    private FarskapsportalService farskapsportalService;

    @Test
    @DisplayName("Skal hente kjoenn for eksisterende person")
    void skalHenteKjoennForEksisterendePerson() {

        var foedselsnummerMor = "01018912345";

        when(pdlApiConsumerMock.henteKjoenn(foedselsnummerMor)).thenReturn(HttpResponse.from(HttpStatus.OK, Kjoenn.KVINNE));

        var response = farskapsportalService.henteKjoenn(foedselsnummerMor);
        var kjoennReturnert = response.getResponseEntity().getBody();

        assertAll(
                () -> assertTrue(response.is2xxSuccessful()),
                () -> assertTrue(Kjoenn.KVINNE.equals(kjoennReturnert))

        );
    }

    @Test
    @DisplayName("Skal gi httpstatuskode 404 dersom informasjon om person mangler")
    void skalGiHttpstatuskode404DersomInformasjonOmPersonMangler() {

        var foedselsnummerMor = "01018912345";

        when(pdlApiConsumerMock.henteKjoenn(foedselsnummerMor)).thenReturn(HttpResponse.from(HttpStatus.NOT_FOUND, Kjoenn.KVINNE));

        var response = farskapsportalService.henteKjoenn(foedselsnummerMor);

        assertTrue(response.getResponseEntity().getStatusCode().is4xxClientError());

    }
}
