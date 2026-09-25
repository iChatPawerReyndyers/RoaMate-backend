package com.roamate.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Plain liveness check at GET /health - public (see SecurityConfig), no
 * database or other dependency touched, so it reflects "is the process up
 * and serving requests" rather than "is everything downstream healthy".
 * That matters for a host like Render: pointing its health check here means
 * a brief database hiccup doesn't get misread as "the app is dead" and
 * trigger a restart.
 *
 * This is deliberately NOT /actuator/health - that path is already
 * permitted in SecurityConfig, but Spring Boot Actuator isn't a dependency
 * in pom.xml, so that path 404s today. Adding Actuator would give richer
 * checks (DB connectivity, disk space) at the cost of a new dependency and
 * more surface to secure; this is the minimal version for now.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}