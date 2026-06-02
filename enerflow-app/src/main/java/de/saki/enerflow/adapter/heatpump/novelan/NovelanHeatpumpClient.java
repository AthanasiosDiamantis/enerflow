package de.saki.enerflow.adapter.heatpump.novelan;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * WebSocket client for the Novelan heat pump (Helox 5, WRP 2.0).
 * Communicates via the Lux_WS protocol on port 8214
 *
 * @author saki
 */
public class NovelanHeatpumpClient extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(NovelanHeatpumpClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> CATEGORIES_OF_INTEREST = Set.of(
            "Temperaturen",
            "Betriebsstunden",
            "Anlagenstatus",
            "Energiemonitor",
            "Betriebsart");

    private final String password;

    public NovelanHeatpumpClient(URI serverUri, String password) {
        // with Draft_6455 we can specify the protocol Lux_WS, because the heatpump expects the Lux_WS protocol
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
        log.debug("Message received: {}", message);

        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.get("type").asString();

            switch (type) {
                case "Navigation" -> handleNavigation(root);
                case "Content" -> handleContent(root);
                case "values" -> handleValues(root);
                default -> log.warn("Unknown message type: {}", type);
            }

        } catch (Exception e) {
            log.error("Error parsing message: {}", e.getMessage(), e);
        }
    }

    private void handleNavigation(JsonNode root) {
        log.info("Navigation received - searching for categories of interest...");
        JsonNode items = root.get("items");
        searchAndFetch(items);
    }

    private void searchAndFetch(JsonNode items) {
        if (items == null || !items.isArray()) {
            log.warn("No items found in navigation");
            return;
        }
        for (JsonNode item : items) {
            String name = item.get("name").asString();
            String id = item.get("id").asString();

            if (CATEGORIES_OF_INTEREST.contains(name)) {
                log.info("Found category: '{}' - sending GET;{}", name, id);
                send("GET;" + id);
            }

            // Recurse into sub-items
            searchAndFetch(item.get("items"));
        }
    }

    private void handleContent(JsonNode root) {
        log.info("Content received:");
        try{
            log.info("\n{}",
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(root));
        } catch (Exception e){
            log.error("Error formatting content: {}", e.getMessage());
        }
    }

    private void handleValues(JsonNode root) {
        log.debug("Realtime values update received");
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
