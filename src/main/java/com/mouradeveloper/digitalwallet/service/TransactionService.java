package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.mapper.TransactionMapper;
import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.entity.Transaction;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.TransactionRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper transactionMapper;


    @Transactional
    public TransactionDto.Response createTransfer(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount
    ) {
        var sourceWallet = findWalletOrThrow(sourceWalletId);
        var destinationWallet = findWalletOrThrow(destinationWalletId);

        var transaction = Transaction.transfer(sourceWallet, destinationWallet, amount);

        sourceWallet.withdraw(amount);
        destinationWallet.deposit(amount);

        var transactionSaved = transactionRepository.save(transaction);

        return transactionMapper.toResponse(transactionSaved);
    }


    private Wallet findWalletOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(
                        () -> new WalletNotFoundException(walletId)
                );
    }
}
