package com.honzel.core.util.web;

import java.io.IOException;

public class HttpResponseMessageException extends IOException {

    private final int responseCode;

    /**
     * Constructs an {@code HttpRequestException} with the given detail message.
     *
     * @param responseCode The HTTP status code
     * @param message
     *        The detail message; can be {@code null}
     */
    public HttpResponseMessageException(int responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
