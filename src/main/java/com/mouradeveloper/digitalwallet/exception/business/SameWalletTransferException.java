package com.mouradeveloper.digitalwallet.exception.business;

public class SameWalletTransferException extends BusinessRuleException {
    public SameWalletTransferException() {
        super("Source and destination wallets must be different.");
    }
}
