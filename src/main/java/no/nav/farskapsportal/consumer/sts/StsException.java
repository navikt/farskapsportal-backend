package no.nav.farskapsportal.consumer.sts;

public class StsException extends RuntimeException {
    public StsException(String message, Throwable cause) {
        super(message, cause);
    }
}