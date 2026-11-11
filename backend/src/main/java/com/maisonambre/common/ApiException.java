package com.maisonambre.common;

import org.springframework.http.HttpStatus;

/**
 * A business error with an HTTP status and a stable code the frontend can rely on.
 *
 * Status codes used by the API:
 * 400 the request is malformed (handled by Spring validation),
 * 404 the resource in the URL does not exist,
 * 409 the request conflicts with the current state (not enough stock, order no longer pending),
 * 422 the request is well formed but breaks a business rule (mixed boutiques, unknown customer).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException unprocessable(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
