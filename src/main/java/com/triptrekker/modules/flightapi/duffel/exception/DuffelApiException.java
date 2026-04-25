package com.triptrekker.modules.flightapi.duffel.exception;

import com.triptrekker.common.exception.BusinessException;
import lombok.Getter;

@Getter
public class DuffelApiException extends BusinessException {

    private final int statusCode;

    public DuffelApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

}
