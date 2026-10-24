package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.exception.notFound.TransactionNotFoundException;
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
import java.util.List;
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
        if (sourceWalletId.equals(destinationWalletId)) {
            throw new SameWalletTransferException();
        }

        var sourceWallet = findWalletOrThrow(sourceWalletId);
        var destinationWallet = findWalletOrThrow(destinationWalletId);

        var transaction = Transaction.transfer(sourceWallet, destinationWallet, amount);

        sourceWallet.withdraw(amount);
        destinationWallet.deposit(amount);

        var transactionSaved = transactionRepository.save(transaction);

        return transactionMapper.toResponse(transactionSaved);
    }

    @Transactional(readOnly = true)
    public TransactionDto.Response findById(Long transactionId) {
        return transactionMapper.toResponse(findTransactionOrThrow(transactionId));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> findByWalletId(UUID walletId) {
        findWalletOrThrow(walletId);

        return transactionRepository.findBySourceWallet_IdOrDestinationWallet_Id(walletId, walletId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }


    private Wallet findWalletOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(
                        () -> new WalletNotFoundException(walletId)
                );
    }

    private Transaction findTransactionOrThrow(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(
                        () -> new TransactionNotFoundException(transactionId)
                );
    }
}
