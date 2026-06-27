package io.mywallet.infrastructure.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminController {

    private final KillSwitchService killSwitchService;

    public AdminController(KillSwitchService killSwitchService) {
        this.killSwitchService = killSwitchService;
    }

    @PostMapping("/emergency-stop")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate the global kill switch - blocks all new orders system-wide until deactivated")
    public void emergencyStop(@RequestBody(required = false) Map<String, String> body, Authentication authentication) {
        String reason = body != null ? body.get("reason") : null;
        killSwitchService.activate(UUID.fromString(authentication.getName()), reason);
    }

    @PostMapping("/emergency-stop/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivateEmergencyStop() {
        killSwitchService.deactivate();
    }

    @GetMapping("/emergency-stop")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> status() {
        return Map.of("enabled", killSwitchService.isEnabled());
    }
}
