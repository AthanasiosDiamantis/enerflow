package de.saki.enerflow.adapter.heatpump.novelan;

import de.saki.enerflow.core.domain.HeatpumpSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;


/**
 * Maps Novelan heat pump JSON content blocks to HeatpumpSnapshot domain objects.
 * Each content block (Temperaturen, Anlagenstatus, etc.) is processed separately
 * and merged into a single snapshot.
 *
 * @author saki
 */
@Component
public class NovelanSnapshotMapper {

    private static final Logger log = LoggerFactory.getLogger(NovelanSnapshotMapper.class);

    /**
     * Processes a single content block from the heat pump and maps
     * the relevant values into the given snapshot.
     *
     * @param snapshot the snapshot to enrich with new values
     * @param root the JSON content block from the heat pump
     */
    public void mapContentBlock(HeatpumpSnapshot snapshot, JsonNode root) {
        String blockName = root.get("name").asString();
        JsonNode items = root.get("items");

        if(items == null || !items.isArray()) {
            log.warn("No items found in content block '{}'", blockName);
            return;
        }

        for(JsonNode item: items) {
            String name = item.get("name").asString();
            String value = item.has("value") ? item.get("value").asString() : null;

            switch(blockName) {
                case "Temperaturen"    -> mapTemperaturItem(snapshot, name, value);
                case "Anlagenstatus"   -> mapAnlagenstatusItem(snapshot, name, value);
                case "Betriebsstunden" -> mapBetriebsstundenItem(snapshot, name, value);
            }
        }
    }

    // --- private mapping methods ---

    private void mapTemperaturItem(HeatpumpSnapshot snapshot, String name, String value) {
        switch (name) {
            case "Vorlauf"          -> snapshot.setVorlaufTemp(parseDouble(value));
            case "Rücklauf"         -> snapshot.setRuecklaufTemp(parseDouble(value));
            case "Warmwasser-Ist"   -> snapshot.setWarmwasserIst(parseDouble(value));
            case "Warmwasser-Soll"  -> snapshot.setWarmwasserSoll(parseDouble(value));
            case "Außentemperatur"  -> snapshot.setAussentemperatur(parseDouble(value));
            case "Hysterese WW"     -> snapshot.setHystereseWwK(parseDouble(value));
        }
    }

    private void mapAnlagenstatusItem(HeatpumpSnapshot snapshot, String name, String value) {
        switch (name) {
            case "Heizleistung Ist"  -> snapshot.setHeizleistungKw(parseDouble(value));
            case "Leistungsaufnahme" -> snapshot.setLeistungsaufnahmeKw(parseDouble(value));
            case "Betriebszustand"   -> snapshot.setBetriebszustand(value);
        }
    }

    private void mapBetriebsstundenItem(HeatpumpSnapshot snapshot, String name, String value) {
        // Novelan uses "Betriebstunden WP" instead of "Betriebsstunden WP", leak of one "s"
        if ("Betriebstunden WP".equals(name)) {
            snapshot.setBetriebsstundenWp(parseInteger(value));
        }
    }

    // --- Parsing helpers, parse correct like 47.0°C to Double 47.0 ---

    /**
     * Extracts a Double from a string like "45.7°C", "0.00 kW", "-2.0°C".
     * Returns null if the value is blank or cannot be parsed.
     */
    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String cleaned = value.replaceAll("[^0-9.\\-]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse double from value: '{}'", value);
            return null;
        }
    }

    /**
     * Extracts an Integer from a string like "5339h", "1753".
     * Returns null if the value is blank or cannot be parsed.
     */
    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String cleaned = value.replaceAll("[^0-9]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse integer from value: '{}'", value);
            return null;
        }
    }

}
