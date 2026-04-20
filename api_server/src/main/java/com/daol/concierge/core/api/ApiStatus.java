package com.daol.concierge.core.api;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApiStatus {
    SUCCESS(0),
    BAD_REQUEST(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    NOT_FOUND_USER(-10),
    INVALID_PASSWORD(-20),
    ACCESS_DENIED(-30),
    METHOD_NOT_ALLOWED(405),
    REDIRECT(302),
    SYSTEM_ERROR(-500);

    private final int code;

    @JsonValue
    public int getCode() { return this.code; }

    ApiStatus(int code) { this.code = code; }

    public static ApiStatus getApiStatus(int code) {
        for (ApiStatus s : values()) {
            if (code == s.getCode()) return s;
        }
        return null;
    }
}
