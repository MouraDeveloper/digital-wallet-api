package com.mouradeveloper.digitalwallet.models.entity;

import com.mouradeveloper.digitalwallet.exception.business.BusinessRuleException;
import com.mouradeveloper.digitalwallet.exception.business.InvalidAmountException;
import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.models.enums.TypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TypeEnum type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id", updatable = false)
    private Wallet sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_wallet_id", nullable = false, updatable = false)
    private Wallet destinationWallet;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @CreatedDate
    @Column(nullable = false, name = "created_at", updatable = false)
    private Instant createdAt;


    private Transaction(
            TypeEnum type,
            Wallet sourceWallet,
            Wallet destinationWallet,
            BigDecimal amount
    ) {
        this.type = type;
        this.sourceWallet = sourceWallet;
        this.destinationWallet = destinationWallet;
        this.amount = amount;
    }


    public static Transaction transfer(
            Wallet source,
            Wallet destination,
            BigDecimal amount
    ) {
        validateAmount(amount);

        if (source == null) {
            throw new BusinessRuleException("Source wallet must not be null for a transfer transaction.");
        }

        if (destination == null) {
            throw new BusinessRuleException("Destination wallet must not be null.");
        }

        if (source == destination) {
            throw new SameWalletTransferException();
        }

        if (source.getId() != null && source.getId().equals(destination.getId())) {
            throw new SameWalletTransferException();
        }

        return new Transaction(
                TypeEnum.TRANSFER,
                source,
                destination,
                amount
        );
    }

    public static Transaction deposit(
            Wallet destination,
            BigDecimal amount
    ) {
        validateAmount(amount);

        if (destination == null) {
            throw new BusinessRuleException("Destination wallet must not be null.");
        }

        return new Transaction(
                TypeEnum.DEPOSIT,
                null,
                destination,
                amount
        );
    }


    private static void validateAmount(BigDecimal amount){
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }

        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidAmountException();
        }
    }
}
