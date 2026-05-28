package io.mywallet.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Home", description = "Root API entry point and status")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "Root API status and helpful links")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
            "name", "MyWallet API",
            "version", "0.1.0",
            "status", "UP",
            "documentation", "/swagger-ui/index.html",
            "swaggerUi", "/swagger-ui.html",
            "health", "/actuator/health",
            "frontend", "http://localhost:5173"
        ));
    }
}
