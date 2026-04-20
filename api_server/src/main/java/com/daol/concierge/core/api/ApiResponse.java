package com.daol.concierge.core.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ApiResponse {
    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private ApiError error = null;

    @JsonProperty("message")
    private String message;

    @JsonProperty("redirect")
    private String redirect = "";

    public ApiResponse() {}

    public ApiResponse(ApiStatus apiStatus, String message, String errorMessage) {
        this.status = apiStatus.getCode();
        this.message = message;
        if (errorMessage != null && !errorMessage.isEmpty()) {
            this.error = ApiError.of(errorMessage);
        }
    }

    public static ApiResponse of(ApiStatus apiStatus, String message) {
        return new ApiResponse(apiStatus, message, null);
    }

    public static ApiResponse error(ApiStatus apiStatus, String errorMessage) {
        return new ApiResponse(apiStatus, "", errorMessage);
    }

    public static ApiResponse redirect(String redirect) {
        ApiResponse r = new ApiResponse();
        r.setRedirect(redirect);
        r.setStatus(ApiStatus.REDIRECT.getCode());
        r.setMessage("redirect");
        return r;
    }
}
