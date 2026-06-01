package de.saki.enerflow.adapter.heatpump.novelan;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

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
        super(serverUri);
        this.password = password;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connected to heat pump at {}", getURI());
        send("LOGIN;" + password);
        log.info("Login sent");
    }

    @Override
    public void onMessage(String message) {
        log.debug("Message received: {}", message);
        //Parsing comes in the next step
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
