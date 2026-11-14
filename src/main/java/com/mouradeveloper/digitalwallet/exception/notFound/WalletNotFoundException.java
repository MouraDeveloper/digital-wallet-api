package com.mouradeveloper.digitalwallet.exception.notFound;

import java.util.UUID;

public class WalletNotFoundException extends NotFoundException {

    public WalletNotFoundException(UUID walletId) {
        super("Wallet with ID " + walletId + " not found.");
    }

    public WalletNotFoundException() {
        super("Wallet not found for authenticated user.");
    }
}
