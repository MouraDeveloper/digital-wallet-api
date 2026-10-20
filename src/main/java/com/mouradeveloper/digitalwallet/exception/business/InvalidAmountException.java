package com.mouradeveloper.digitalwallet.exception.business;

public class InvalidAmountException extends BusinessRuleException {
    public InvalidAmountException() {
        super("Amount must be greater than zero.");
    }
}
