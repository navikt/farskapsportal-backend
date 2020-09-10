package no.nav.farskapsportal.consumer.sts.nav;

import no.nav.farskapsportal.consumer.sts.AbstractRefreshingSts;

public class NavSts extends AbstractRefreshingSts {
    public NavSts(NavStsTokenSupplier tokenSupplier, Long refreshOnSecondsLeft) {
        super(tokenSupplier, refreshOnSecondsLeft);
    }
}
