package com.mouradeveloper.digitalwallet.exception.notFound;

public class TransactionNotFoundException extends NotFoundException {
    public TransactionNotFoundException(Long transactionId) {
        super("Transaction with ID " + transactionId + " not found.");
    }
}
