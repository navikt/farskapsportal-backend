package no.nav.farskapsportal.consumer.pdl;

import java.util.List;
import java.util.StringJoiner;

public class PdlApiErrorsException extends RuntimeException {

    private String message;

    public PdlApiErrorsException(List<String> errors) {
        super();
        StringJoiner stringJoiner = new StringJoiner("\n");
        stringJoiner.add("Error i respons fra pdl-api: ");
        errors.forEach(stringJoiner::add);
        message = stringJoiner.toString();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
