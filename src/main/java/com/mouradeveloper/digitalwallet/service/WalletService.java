package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.mapper.WalletMapper;
import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
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
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;
    private final TransactionRepository transactionRepository;


    @Transactional(readOnly = true)
    public WalletDto.Response findById(UUID walletId) {
        var wallet = findWalletOrThrow(walletId);

        return walletMapper.toResponse(wallet);
    }


    @Transactional
    public WalletDto.Response deposit(UUID walletId, BigDecimal amount) {
        var wallet = findWalletOrThrow(walletId);

        wallet.deposit(amount);

        var transaction = Transaction.deposit(wallet, amount);

        transactionRepository.save(transaction);

        return walletMapper.toResponse(wallet);
    }


    private Wallet findWalletOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(
                        () -> new WalletNotFoundException(walletId)
                );
    }
}
