package com.daol.concierge.core.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "of")
public class ApiError {
    @JsonProperty("message")
    @NonNull
    private String message;

    @JsonProperty("requiredKey")
    private String requiredKey;
}
