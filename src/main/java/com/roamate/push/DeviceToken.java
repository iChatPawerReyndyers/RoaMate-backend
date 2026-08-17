package com.roamate.push;

import com.roamate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** GEO-02/03: a device's FCM/APNs push token, used to address a silent location-request push at it. */
@Entity
@Table(name = "device_tokens")
public class DeviceToken extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, unique = true, length = 4096)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    public enum Platform { ANDROID, IOS }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
}
