package no.nav.farskapsportal.consumer.sts;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRefreshingSts {
    private final TokenSupplier tokenSupplier;
    private final Long refreshOnSecondsLeft;

    protected TokenWrapper wrapper;

    public String getToken() {
        updateTokenIfNeeded();
        return wrapper.getToken();
    }

    public String getBearerToken() {
        return "Bearer " + getToken();
    }

    private void updateTokenIfNeeded() {
        if (shouldRefresh()) {
            synchronized (this) {
                if (shouldRefresh()) {
                    try {
                        wrapper = tokenSupplier.fetchToken();
                    } catch (RuntimeException e) {
                        val type = this.getClass().getSimpleName();
                        if (hasExpired()) {
                            throw new StsException(String.format("Failed to refresh token for %s", type), e);
                        } else {
                            log.warn(String.format("Failed to refresh token for %s. Will use existing token instead", type), e);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("findbugs:IS2_INCONSISTENT_SYNC")
    private boolean shouldRefresh() {
        if (wrapper != null && wrapper.getExpiry() != null) {
            return LocalDateTime.now().plusSeconds(refreshOnSecondsLeft).isAfter(wrapper.getExpiry());
        }
        return true;
    }

    private boolean hasExpired() {
        if (wrapper != null && wrapper.getExpiry() != null) {
            return LocalDateTime.now().isAfter(wrapper.getExpiry());
        }
        return true;
    }
}
