package com.mouradeveloper.digitalwallet.models.entity;

import com.mouradeveloper.digitalwallet.exception.business.BusinessRuleException;
import com.mouradeveloper.digitalwallet.exception.business.InvalidAmountException;
import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.models.enums.TypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class TransactionTest {

    @Test
    void shouldCreateDepositTransactionWhenDataIsValid() {
        var appUser = createAppUserValid();
        var wallet = createWallet(appUser);
        var amount = BigDecimal.valueOf(100.00);

        var transaction = Transaction.deposit(wallet, amount);

        Assertions.assertEquals(TypeEnum.DEPOSIT, transaction.getType());

        Assertions.assertEquals(wallet, transaction.getDestinationWallet());

        Assertions.assertNull(transaction.getSourceWallet());

        Assertions.assertEquals(amount, transaction.getAmount());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenDepositDestinationWalletIsNull() {
        Wallet walletDestination = null;

        Assertions.assertThrows(
                BusinessRuleException.class, () -> Transaction.deposit(walletDestination, BigDecimal.valueOf(100.00))
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountIsNull() {
        var walletDestination = createWallet(createAppUserValid());
        BigDecimal amount = null;

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.deposit(walletDestination, amount)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountIsZero() {
        var walletDestination = createWallet(createAppUserValid());
        BigDecimal amount = BigDecimal.ZERO;

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.deposit(walletDestination, amount)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountIsNegative() {
        var walletDestination = createWallet(createAppUserValid());
        BigDecimal amount = BigDecimal.valueOf(-100.00);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.deposit(walletDestination, amount)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountHasMoreThanTwoDecimalPlaces() {
        var walletDestination = createWallet(createAppUserValid());
        BigDecimal amount = BigDecimal.valueOf(100.003);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.deposit(walletDestination, amount)
        );
    }

    @Test
    void shouldCreateTransferTransactionWhenDataIsValid() {
        var appUser = createAppUserValid();
        var appUser2 = createAppUserValid();
        var walletSource = createWallet(appUser);
        var walletDestination = createWallet(appUser2);
        var amount = BigDecimal.valueOf(100.00);

        var transaction = Transaction.transfer(walletSource, walletDestination, amount);

        Assertions.assertEquals(TypeEnum.TRANSFER, transaction.getType());
        Assertions.assertEquals(walletSource, transaction.getSourceWallet());
        Assertions.assertEquals(walletDestination, transaction.getDestinationWallet());
        Assertions.assertEquals(amount, transaction.getAmount());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenTransferSourceWalletIsNull() {
        var appUser2 = createAppUserValid();
        Wallet sourceWallet = null;
        var destinationWallet = createWallet(appUser2);
        var amount = BigDecimal.valueOf(100.00);

        Assertions.assertThrows(
                BusinessRuleException.class, () -> Transaction.transfer(sourceWallet, destinationWallet, amount)
        );

        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(destinationWallet.getBalance()));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenTransferDestinationWalletIsNull() {
        var appUser2 = createAppUserValid();
        var sourceWallet = createWallet(appUser2);
        Wallet destinationWallet = null;
        var amount = BigDecimal.valueOf(100.00);

        Assertions.assertThrows(
                BusinessRuleException.class, () -> Transaction.transfer(sourceWallet, destinationWallet, amount)
        );

        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(sourceWallet.getBalance()));
    }

    @Test
    void shouldThrowSameWalletTransferExceptionWhenSourceAndDestinationAreTheSame() {
        var appUser2 = createAppUserValid();
        var sourceWallet = createWallet(appUser2);
        Wallet destinationWallet = sourceWallet;
        var amount = BigDecimal.valueOf(100.00);

        Assertions.assertThrows(
                SameWalletTransferException.class, () -> Transaction.transfer(sourceWallet, destinationWallet, amount)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenTransferAmountIsNull() {
        var sourceWallet = createWallet(createAppUserValid());
        var destinationWallet = createWallet(createAppUserValid());
        BigDecimal amount = null;

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.transfer(sourceWallet, destinationWallet, amount)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenTransferAmountIsZero() {
        var sourceWallet = createWallet(createAppUserValid());
        var destinationWallet = createWallet(createAppUserValid());
        BigDecimal amount = BigDecimal.ZERO;

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.transfer(sourceWallet, destinationWallet, amount)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenTransferAmountIsNegative() {
        var sourceWallet = createWallet(createAppUserValid());
        var destinationWallet = createWallet(createAppUserValid());
        BigDecimal amount = BigDecimal.valueOf(-100.00);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.transfer(sourceWallet, destinationWallet, amount)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenTransferAmountHasMoreThanTwoDecimalPlaces() {
        var sourceWallet = createWallet(createAppUserValid());
        var destinationWallet = createWallet(createAppUserValid());
        BigDecimal amount = BigDecimal.valueOf(100.002);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> Transaction.transfer(sourceWallet, destinationWallet, amount)
        );
    }


    private AppUser createAppUserValid() {
        return AppUser.create(
                "John Doe",
                "john@gmail.com",
                "password123"
        );
    }

    private Wallet createWallet(AppUser appUser) {
        return Wallet.create(appUser);
    }
}
