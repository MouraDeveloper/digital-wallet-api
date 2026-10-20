package com.mouradeveloper.digitalwallet.exception.business;

public class InsufficientFundsException extends BusinessRuleException {
    public InsufficientFundsException() {
        super("Insufficient funds.");
    }
}
