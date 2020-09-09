package no.nav.farskapsportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FarskapsportalApiApplication {
    public static final String ISSUER = "isso";

    public static void main(String[] args) {
        SpringApplication.run(FarskapsportalApiApplication.class, args);
    }

}
