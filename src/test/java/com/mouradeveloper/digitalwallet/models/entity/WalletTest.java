package com.mouradeveloper.digitalwallet.models.entity;

import com.mouradeveloper.digitalwallet.exception.business.BusinessRuleException;
import com.mouradeveloper.digitalwallet.exception.business.InsufficientFundsException;
import com.mouradeveloper.digitalwallet.exception.business.InvalidAmountException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class WalletTest {

    @Test
    void shouldCreateWalletWithZeroBalanceWhenUserIsValid() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        Assertions.assertEquals(appUser, wallet.getUser());
        Assertions.assertEquals(BigDecimal.ZERO, wallet.getBalance());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenUserIsNull() {
        Assertions.assertThrows(
                BusinessRuleException.class, () -> Wallet.create(null)
        );
    }

    @Test
    void shouldIncreaseBalanceWhenDepositAmountIsValid() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        wallet.deposit(BigDecimal.valueOf(100.00));

        Assertions.assertEquals(BigDecimal.valueOf(100.00), wallet.getBalance());
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountIsNegative() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.deposit(BigDecimal.valueOf(-100.00))
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountIsZero() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.deposit(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountIsNull() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.deposit(null)
        );
    }

    @Test
    void shouldDecreaseBalanceWhenWithdrawAmountIsValid() {
        var appUser = createAppUserValid();
        var wallet = Wallet.create(appUser);
        var amount = BigDecimal.valueOf(100.00);

        wallet.deposit(amount);
        wallet.withdraw(amount);

        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
    }

    @Test
    void shouldThrowInsufficientFundsExceptionWhenWithdrawAmountExceedsBalance() {
        var appUser = createAppUserValid();
        var amount = BigDecimal.valueOf(80.00);
        var wallet = Wallet.create(appUser);
        wallet.deposit(amount);

        Assertions.assertThrows(
                InsufficientFundsException.class, () -> wallet.withdraw(BigDecimal.valueOf(100.00))
        );

        Assertions.assertEquals(amount, wallet.getBalance());
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenWithdrawAmountIsNull() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.withdraw(null)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenWithdrawAmountIsZero() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.withdraw(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenWithdrawAmountIsNegative() {
        var appUser = createAppUserValid();

        var wallet = Wallet.create(appUser);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.withdraw(BigDecimal.valueOf(-100.00))
        );
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenWithdrawAmountHasMoreThanTwoDecimalPlaces() {
        var appUser = createAppUserValid();
        var wallet = Wallet.create(appUser);
        var amount = BigDecimal.valueOf(200.00);
        wallet.deposit(amount);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.withdraw(BigDecimal.valueOf(100.003))
        );

        Assertions.assertEquals(BigDecimal.valueOf(200.00), wallet.getBalance());
    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenDepositAmountHasMoreThanTwoDecimalPlaces() {
        var appUser = createAppUserValid();
        var wallet = Wallet.create(appUser);
        var amount = BigDecimal.valueOf(200.00);
        wallet.deposit(amount);

        Assertions.assertThrows(
                InvalidAmountException.class, () -> wallet.deposit(BigDecimal.valueOf(100.003))
        );

        Assertions.assertEquals(BigDecimal.valueOf(200.00), wallet.getBalance());
    }


    private AppUser createAppUserValid() {
        var fullName = "John Doe";
        var email = "john123@gmail.com";
        var passwordHash = "password123";

        return AppUser.create(fullName, email, passwordHash);
    }
}
