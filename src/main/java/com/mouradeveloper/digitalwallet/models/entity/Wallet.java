package com.mouradeveloper.digitalwallet.models.entity;

import com.mouradeveloper.digitalwallet.exception.business.BusinessRuleException;
import com.mouradeveloper.digitalwallet.exception.business.InsufficientFundsException;
import com.mouradeveloper.digitalwallet.exception.business.InvalidAmountException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(precision =  12, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;


    private Wallet(
            AppUser user
    ){
        this.user = user;
    }


    public static Wallet create(AppUser user){
        if (user == null){
            throw new BusinessRuleException("User cannot be null when creating a wallet.");
        }

        return new Wallet(user);
    }

    public void deposit(BigDecimal amount){
        validateAmount(amount);

        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount){
        validateAmount(amount);

        if(amount.compareTo(this.balance) > 0){
            throw new InsufficientFundsException();
        }

        this.balance = this.balance.subtract(amount);
    }


    private static void validateAmount(BigDecimal amount){
        if (amount == null){
            throw new InvalidAmountException();
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new InvalidAmountException();
        }

        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidAmountException();
        }
    }
}
