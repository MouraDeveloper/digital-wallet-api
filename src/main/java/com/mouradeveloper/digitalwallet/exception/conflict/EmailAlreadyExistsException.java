package com.mouradeveloper.digitalwallet.exception.conflict;

public class EmailAlreadyExistsException extends ConflictException {
    public EmailAlreadyExistsException(String email) {
        super("Email " + email + " already exists.");
    }
}
