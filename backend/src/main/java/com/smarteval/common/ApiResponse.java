package com.smarteval.common;

import java.util.Map;

public record ApiResponse<T>(boolean success, T data, String message, Map<String, String> errors) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "", Map.of());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, Map.of());
    }

    public static <T> ApiResponse<T> fail(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, null, message, errors);
    }
}
