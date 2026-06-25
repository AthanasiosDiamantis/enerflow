package de.saki.enerflow.api.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST controller exposing dashboard data for the frontend.
 * Polled every 10 seconds by the Vanilla JS client (US-01-01).
 *
 * @author saki
 */
@Controller
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Returns the current system status as a JSON snapshot.
     * Aggregates battery, heat pump, EnerFlow state and savings data.
     *
     * @return 200 OK with {@link DashboardStatusDto} as JSON body
     */
    @GetMapping("/status")
    public ResponseEntity<DashboardStatusDto> getStatus() {
        return ResponseEntity.ok(dashboardService.getDashboardStatus());
    }

}
