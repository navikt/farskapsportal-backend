package no.nav.farskapsportal.consumer.sts;

public interface TokenSupplier {
    TokenWrapper fetchToken();
}