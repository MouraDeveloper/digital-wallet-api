package com.mouradeveloper.digitalwallet.exception.notFound;

import java.util.UUID;

public class AppUserNotFoundException extends NotFoundException {
    public AppUserNotFoundException(UUID userId) {
        super("User with ID " + userId + " not found.");
    }
}
