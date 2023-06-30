package no.nav.farskapsportal.backend.apps.api.exception;

import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;

public class ArkiveringFeiletException extends InternFeilException {
    public ArkiveringFeiletException(Feilkode feilkode) {
        super(feilkode);
    }
}
