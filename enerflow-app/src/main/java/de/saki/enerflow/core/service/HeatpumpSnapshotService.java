package de.saki.enerflow.core.service;

import de.saki.enerflow.adapter.heatpump.novelan.NovelanSnapshotMapper;
import de.saki.enerflow.core.domain.HeatpumpSnapshot;
import de.saki.enerflow.core.repository.HeatpumpSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the collection of heat pump data blocks,
 * maps them into a HeatpumpSnapshot and persists it to the database.
 *
 * @author saki
 */
@Service
public class HeatpumpSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(HeatpumpSnapshotService.class);

    private final NovelanSnapshotMapper mapper;
    private final HeatpumpSnapshotRepository repository;

    /**
     * Collects content blocks until snapshot is complete.
     */
    private final List<JsonNode> pendingBlocks = new ArrayList<>();
    private int expectedBlocks = 0;

    public HeatpumpSnapshotService(
            NovelanSnapshotMapper mapper,
            HeatpumpSnapshotRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    public void setExpectedBlocks(int count) {
        this.expectedBlocks = count;
        this.pendingBlocks.clear();
        log.debug("Expected {} content blocks for next snapshot", count);
    }

    /**
     * Receives a content block from the heat pump client.
     * Automatically saves when all expected blocks have arrived.
     *
     * @param block the JSON content block from the heat pump
     */
    public void addContentBlock(JsonNode block) {
        String blockName = block.get("name").asString();
        log.debug("Content block received: {} ({}/{})",
                blockName,
                pendingBlocks.size() + 1,
                expectedBlocks);
        pendingBlocks.add(block);

        if(pendingBlocks.size() >= expectedBlocks) {
            flushAndSave();
        }
    }

    /**
     * Builds a complete HeatpumpSnapshot from all pending blocks,
     * persists it and clears the pending list.
     * Called after all GET responses have been received.
     */
    public void flushAndSave() {
        HeatpumpSnapshot snapshot = new HeatpumpSnapshot();
        snapshot.setTimestamp(LocalDateTime.now());

        for(JsonNode block: pendingBlocks) {
            mapper.mapContentBlock(snapshot, block);
        }

        repository.save(snapshot);
        log.info("Heatpump snapshot saved: " +
                        "warmwasser-ist={}°C, warmwasser-soll={}°C, aussentemp={}°C",
                snapshot.getWarmwasserIst(),
                snapshot.getWarmwasserSoll(),
                snapshot.getAussentemperatur());

        pendingBlocks.clear();
        expectedBlocks = 0;

    }



}
