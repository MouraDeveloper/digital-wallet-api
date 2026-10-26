package com.mouradeveloper.digitalwallet.models.entity;

import com.mouradeveloper.digitalwallet.exception.business.BusinessRuleException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AppUserTest {

    @Test
    void shouldCreateAppUserWhenDataIsValid() {
        var fullName = "John Doe";
        var email = "john123@gmail.com";
        var passwordHash = "password123";

        var appUser = AppUser.create(fullName, email, passwordHash);

        Assertions.assertEquals(fullName, appUser.getFullName());
        Assertions.assertEquals(email, appUser.getEmail());
        Assertions.assertEquals(passwordHash, appUser.getPasswordHash());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenFullNameIsNull() {
        String fullName = null;
        var email = "email123@gmail.com";
        var passwordHash = "password123";

        Assertions.assertThrows(
                BusinessRuleException.class, () -> AppUser.create(fullName, email, passwordHash)
        );
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenFullNameIsBlank() {
        String fullName = " ";
        var email = "email123@gmail.com";
        var passwordHash = "password123";

        Assertions.assertThrows(
                BusinessRuleException.class, () -> AppUser.create(fullName, email, passwordHash)
        );
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenEmailIsNull() {
        var fullName = "John Doe";
        String email = null;
        var passwordHash = "password123";

        Assertions.assertThrows(
                BusinessRuleException.class, () -> AppUser.create(fullName, email, passwordHash)
        );
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenEmailIsBlank() {
        var fullName = "John Doe";
        String email = " ";
        var passwordHash = "password123";

        Assertions.assertThrows(
                BusinessRuleException.class, () -> AppUser.create(fullName, email, passwordHash)
        );
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenPasswordHashIsNull() {
        var fullName = "John Doe";
        var email = "email12334@gmail.com";
        String passwordHash = null;

        Assertions.assertThrows(
                BusinessRuleException.class, () -> AppUser.create(fullName, email, passwordHash)
        );
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenPasswordHashIsBlank() {
        var fullName = "John Doe";
        var email = "email12334@gmail.com";
        String passwordHash = " ";

        Assertions.assertThrows(
                BusinessRuleException.class, () -> AppUser.create(fullName, email, passwordHash)
        );
    }
}
