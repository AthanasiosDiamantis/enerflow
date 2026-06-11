package de.saki.enerflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EnerflowAppApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring context loads without errors
        // Uses the "test" profile which disables DB and external connections
    }

}
