package de.saki.enerflow.adapter.heatpump.novelan;

import de.saki.enerflow.core.model.HeatGenerator;
import de.saki.enerflow.core.service.HeatpumpSnapshotService;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket client for the Novelan heat pump (Helox 5, WRP 2.0).
 * Communicates via the Lux_WS protocol on port 8214
 *
 * @author saki
 */
public class NovelanHeatpumpClient extends WebSocketClient implements HeatGenerator {

    private static final Logger log = LoggerFactory.getLogger(NovelanHeatpumpClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Categories we want to read from the heat pump
    private static final Set<String> CATEGORIES_OF_INTEREST = Set.of(
            "Temperaturen",
            "Betriebsstunden",
            "Anlagenstatus",
            "Energiemonitor",
            "Betriebsart");

    private final String password;
    private final HeatpumpSnapshotService snapshotService;

    // Current ID of the writable hot water setpoint (changes with every REFRESH)
    private volatile String warmwasserSollId = null;

    private final NovelanSnapshotMapper mapper;

    // Tracks how many GET commands were sent per REFRESH cycle
    private final AtomicInteger sentGetCount = new AtomicInteger(0);

    public NovelanHeatpumpClient(URI serverUri,
                                 String password,
                                 HeatpumpSnapshotService snapshotService, NovelanSnapshotMapper mapper) {
        // with Draft_6455 we can specify the protocol Lux_WS, because the heatpump expects the Lux_WS protocol
        super(serverUri, new Draft_6455(
                Collections.emptyList(), // no extensions
                Collections.singletonList(new Protocol("Lux_WS")) // only subprotocol Lux_WS
        ));
        this.password = password;
        this.snapshotService = snapshotService;
        this.mapper = mapper;
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
        sentGetCount.set(0);
        searchAndFetch(root.get("items"));
        snapshotService.setExpectedBlocks(sentGetCount.get());
        log.info("Sent {} GET commands, expecting {} content blocks",
                sentGetCount.get(), sentGetCount.get());
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
                sentGetCount.incrementAndGet();
            }

            // Recurse into sub-items
            searchAndFetch(item.get("items"));
        }
    }

    private void handleContent(JsonNode root) {
        log.info("Content received: {}", root.get("name").asString());

        // Extract writable setpoint ID from Einstellungen-Temperaturen block
        String extractedId = mapper.extractWarmwasserSollId(root);
        if (extractedId != null) {
            warmwasserSollId = extractedId;
            log.info("Warmwasser-Soll ID updated: {}", warmwasserSollId);
        }

        snapshotService.addContentBlock(root);
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

    /**
     * Sets the hot water target temperature on the heat pump.
     * Safety limits are enforced before sending the SET command.
     *
     * @param targetCelsius the desired temperature in degrees Celsius
     * @param minCelsius    the minimum allowed temperature (safety limit)
     * @param maxCelsius    the maximum allowed temperature (safety limit)
     * @throws IllegalStateException    if not connected or ID not yet known
     * @throws IllegalArgumentException if target is outside safety limits
     */
    public void setWarmwasserSolltemperatur(double targetCelsius,
                                            double minCelsius,
                                            double maxCelsius) {
        if (!isOpen()) {
            throw new IllegalStateException("Not connected to heat pump");
        }
        if (warmwasserSollId == null) {
            throw new IllegalStateException(
                    "Warmwasser-Soll ID not yet known - wait for first REFRESH"
            );
        }
        if (targetCelsius < minCelsius || targetCelsius > maxCelsius) {
            throw new IllegalArgumentException(
                    String.format("Target temperature %.1f°C is outside safety limits [%.1f, %.1f]",
                            targetCelsius, minCelsius, maxCelsius)
            );
        }

        // Convert to raw value: Celsius * div (div=10 for this slider)
        int rawValue = (int) Math.round(targetCelsius * 10);
        String command = "SET;" + warmwasserSollId + ";" + rawValue;

        log.info("Setting Warmwasser-Soll to {}°C (raw={}, command={})",
                targetCelsius, rawValue, command);
        send(command);
    }

    // --- HeatGenerator Interface Implementation ---

    @Override
    public double getCurrentTemperatureCelsius() {
        // Returns the last known actual temperature from the snapshot service
        return snapshotService.getLastWarmwasserIst();
    }

    @Override
    public double getSetpointTemperatureCelsius() {
        // Returns the last known setpoint from the snapshot service
        return snapshotService.getLastWarmwasserSoll();
    }

    @Override
    public void setSetpointTemperatureCelsius(double targetTemperatureCelsius) {
        // Safety limits from Pflichtenheft: Min 45°C / Max 60°C
        setWarmwasserSolltemperatur(targetTemperatureCelsius, 45.0, 60.0);
    }

    @Override
    public boolean isAvailable() {
        return isOpen() && warmwasserSollId != null;
    }
}
