package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Beskjedprodusent")
@SpringBootTest(classes = Beskjedprodusent.class)
@ActiveProfiles(PROFILE_TEST)
public class BeskjedprodusentTest {

}
