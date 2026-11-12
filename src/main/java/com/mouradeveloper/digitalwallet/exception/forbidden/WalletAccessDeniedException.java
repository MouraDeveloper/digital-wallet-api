package com.mouradeveloper.digitalwallet.exception.forbidden;

import java.util.UUID;

public class WalletAccessDeniedException extends ForbiddenException {
    public WalletAccessDeniedException(UUID walletId) {
        super("Access denied to wallet: " + walletId);
    }
}
