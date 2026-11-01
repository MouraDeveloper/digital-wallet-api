package com.mouradeveloper.digitalwallet.exception.conflict;

public class IdempotencyConflictException extends ConflictException {

    public IdempotencyConflictException() {
        super("Idempotency key already used with different request data.");
    }
}