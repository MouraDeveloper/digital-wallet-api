package com.mouradeveloper.digitalwallet.exception.badRequest;

public class InvalidIdempotencyKeyException extends BadRequestException {
    public InvalidIdempotencyKeyException() {
        super("Idempotency key must not be blank or null");
    }
}
