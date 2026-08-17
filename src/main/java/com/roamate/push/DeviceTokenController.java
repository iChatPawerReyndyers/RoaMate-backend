package com.roamate.push;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/push/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService service;

    public DeviceTokenController(DeviceTokenService service) {
        this.service = service;
    }

    public record RegisterRequest(String token, DeviceToken.Platform platform) {}
    public record UnregisterRequest(String token) {}

    @PostMapping
    public void register(@RequestBody RegisterRequest request,
                          @AuthenticationPrincipal(expression = "subject") String userId) {
        service.register(userId, request.token(), request.platform());
    }

    /** Called on logout, so a stale token doesn't keep silently addressing pushes to a signed-out account. */
    @PostMapping("/unregister")
    public void unregister(@RequestBody UnregisterRequest request) {
        service.unregister(request.token());
    }
}
