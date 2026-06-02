package de.saki.enerflow.adapter.heatpump.novelan;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;

/**
 * WebSocket client for the Novelan heat pump (Helox 5, WRP 2.0).
 * Communicates via the Lux_WS protocol on port 8214
 *
 * @author saki
 */
public class NovelanHeatpumpClient extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(NovelanHeatpumpClient.class);

    private final String password;

    public NovelanHeatpumpClient(URI serverUri, String password) {
        // with Draft_6455 we can specify the protocol Lux_WS, because the heatpump expects this protocol
        super(serverUri, new Draft_6455(
                Collections.emptyList(), // no extensions
                Collections.singletonList(new Protocol("Lux_WS")) // only subprotocol Lux_WS
        ));
        this.password = password;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("Connected to heat pump at {}", getURI());
        log.info("Handshake HTTP status: {}", handshake.getHttpStatus());
        log.info("Sending login...");
        send("LOGIN;" + password);
    }

    @Override
    public void onMessage(String message) {
        log.info("RAW message received: [{}]", message);

        log.debug("Message received: {}", message);

        // After login the pump sends a short confirmation
        // we respond immediately with a REFRESH command to get the navigation structure
        if(!message.startsWith("{")){
            log.info("Login response received: {}", message);
            log.info("Sending REFRESH...");
            send("REFRESH");
            return;
        }

        // JSON messages from the pump
        if(message.contains("\"Navigation\"")){
            log.info("Navigation received (IDs available)");
        } else if (message.contains("\"Content\"")) {
            log.info("Content received (sensor values)");
        } else if (message.contains("\"values\"")) {
            log.info("Realtime values received");
        }


    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("Heat pump connection closed. Reason: {}, Remote: {}", reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error: {}", ex.getMessage(), ex);
    }
}
