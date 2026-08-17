package com.roamate.push;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    public DeviceTokenService(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Upserts by token, not by (userId, platform): a token uniquely
     * identifies one app install, and if it's already registered - to this
     * same user (token refreshed) or a different one (logout/login on a
     * shared device) - the existing row just gets its owner/platform
     * updated rather than creating a duplicate.
     */
    @Transactional
    public void register(String userId, String token, DeviceToken.Platform platform) {
        DeviceToken deviceToken = repository.findByToken(token).orElseGet(DeviceToken::new);
        deviceToken.setUserId(userId);
        deviceToken.setToken(token);
        deviceToken.setPlatform(platform);
        repository.save(deviceToken);
    }

    @Transactional
    public void unregister(String token) {
        repository.findByToken(token).ifPresent(repository::delete);
    }
}
