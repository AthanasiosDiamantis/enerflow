package de.saki.enerflow.adapter.heatpump.myuplink;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.saki.enerflow.config.MyUplinkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Manages OAuth2 Client Credentials token lifecycle for the myUplink REST API.
 * Fetches a new token when needed and caches it until near expiry.
 *
 * @author saki
 */
@Component
public class MyUplinkTokenService {

    private static final Logger log = LoggerFactory.getLogger(MyUplinkTokenService.class);

    private final MyUplinkProperties properties;
    private final RestClient restClient;

    // Cached token state
    private String cachedToken;
    private Instant tokenExpiresAt;

    public MyUplinkTokenService(MyUplinkProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public String getValidToken() {
        if (needsRefresh()) {
            log.debug("Token missing or near expiry - fetching new token...");
            fetchAndCacheToken();
        }
        return cachedToken;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean needsRefresh() {
        if (cachedToken == null || tokenExpiresAt == null) {
            return true;
        }
        Instant refreshBoundary = tokenExpiresAt.minusSeconds(properties.getTokenRefreshThresholdSeconds());
        return Instant.now().isAfter(refreshBoundary);
    }

    private void fetchAndCacheToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "clienat_credentials");
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("scope", "READSYSTEM WRITESYSTEM");

        TokenResponse response = restClient.post()
                .uri(properties.getBaseUrl() + "/oaouth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(formData)
                .retrieve()
                .body(TokenResponse.class);

        if(response == null || response.accessToken() == null) {
            throw new IllegalStateException("myUplink token response ws null or missing access token");
        }

        cachedToken = response.accessToken();
        tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn());
        log.info("myUplink token refreshed, valid until {}", tokenExpiresAt);
    }

    // -------------------------------------------------------------------------
    // Internal DTO — only used by this class
    // -------------------------------------------------------------------------

    /**
     * Maps the JSON token response from the myUplink OAuth2 endpoint.
     * Field names match the snake_case keys in the API response.
     */
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn) {
    }
}
