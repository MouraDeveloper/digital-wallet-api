
package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.badRequest.InvalidIdempotencyKeyException;
import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.exception.conflict.IdempotencyConflictException;
import com.mouradeveloper.digitalwallet.exception.forbidden.WalletAccessDeniedException;
import com.mouradeveloper.digitalwallet.exception.notFound.TransactionNotFoundException;
import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.mapper.TransactionMapper;
import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.entity.Transaction;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.models.enums.TypeEnum;
import com.mouradeveloper.digitalwallet.repository.TransactionRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import com.mouradeveloper.digitalwallet.security.AuthenticatedUserProvider;
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
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public TransactionDto.Response createTransfer(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException();
        }

        var existingTransaction =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            return validateExistingTransfer(
                    existingTransaction.get(),
                    sourceWalletId,
                    destinationWalletId,
                    amount
            );
        }

        if (sourceWalletId.equals(destinationWalletId)) {
            throw new SameWalletTransferException();
        }

        Wallet sourceWallet;
        Wallet destinationWallet;

        if (sourceWalletId.compareTo(destinationWalletId) < 0) {
            sourceWallet = findWalletForUpdateOrThrow(sourceWalletId);
            destinationWallet = findWalletForUpdateOrThrow(destinationWalletId);
        } else {
            destinationWallet = findWalletForUpdateOrThrow(destinationWalletId);
            sourceWallet = findWalletForUpdateOrThrow(sourceWalletId);
        }

        var ownerId = sourceWallet.getUser().getId();

        if (!authenticatedUserProvider.getUserId().equals(ownerId)) {
            throw new WalletAccessDeniedException(sourceWallet.getId());
        }

        var existingTransactionAfterLock =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransactionAfterLock.isPresent()) {
            return validateExistingTransfer(
                    existingTransactionAfterLock.get(),
                    sourceWalletId,
                    destinationWalletId,
                    amount
            );
        }

        var transaction = Transaction.transfer(
                sourceWallet,
                destinationWallet,
                amount,
                idempotencyKey
        );

        sourceWallet.withdraw(amount);
        destinationWallet.deposit(amount);

        var transactionSaved = transactionRepository.save(transaction);

        return transactionMapper.toResponse(transactionSaved);
    }

    @Transactional(readOnly = true)
    public TransactionDto.Response findById(Long transactionId) {
        Transaction transaction = findTransactionOrThrow(transactionId);

        UUID authenticatedUserId = authenticatedUserProvider.getUserId();

        UUID destinationOwnerId = transaction.getDestinationWallet()
                .getUser()
                .getId();

        boolean isDestinationOwner = authenticatedUserId.equals(destinationOwnerId);

        boolean isSourceOwner =
                transaction.getType() == TypeEnum.TRANSFER
                        && authenticatedUserId.equals(
                        transaction.getSourceWallet()
                                .getUser()
                                .getId()
                );

        boolean canViewDeposit =
                transaction.getType() == TypeEnum.DEPOSIT
                        && isDestinationOwner;

        boolean canViewTransfer =
                transaction.getType() == TypeEnum.TRANSFER
                        && (isSourceOwner || isDestinationOwner);

        if (!canViewDeposit && !canViewTransfer) {
            throw new WalletAccessDeniedException(
                    transaction.getDestinationWallet().getId()
            );
        }

        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> findByWalletId(UUID walletId) {
        Wallet wallet = findWalletOrThrow(walletId);

        UUID ownerId = wallet.getUser().getId();

        if (!authenticatedUserProvider.getUserId().equals(ownerId)) {
            throw new WalletAccessDeniedException(walletId);
        }

        return transactionRepository
                .findBySourceWallet_IdOrDestinationWallet_Id(walletId, walletId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @Transactional
    public TransactionDto.Response createDeposit(
            UUID destinationWalletId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException();
        }

        var existingTransaction =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            return validateExistingDeposit(
                    existingTransaction.get(),
                    destinationWalletId,
                    amount
            );
        }

        var destinationWallet = findWalletForUpdateOrThrow(destinationWalletId);

        var ownerId = destinationWallet.getUser().getId();

        if (!authenticatedUserProvider.getUserId().equals(ownerId)) {
            throw new WalletAccessDeniedException(destinationWallet.getId());
        }

        var existingTransactionAfterLock =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransactionAfterLock.isPresent()) {
            return validateExistingDeposit(
                    existingTransactionAfterLock.get(),
                    destinationWalletId,
                    amount
            );
        }

        var transaction = Transaction.deposit(
                destinationWallet,
                amount,
                idempotencyKey
        );

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

    private Transaction findTransactionOrThrow(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(
                        () -> new TransactionNotFoundException(transactionId)
                );
    }

    private Wallet findWalletForUpdateOrThrow(UUID walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(
                        () -> new WalletNotFoundException(walletId)
                );
    }

    private TransactionDto.Response validateExistingDeposit(
            Transaction existingTransaction,
            UUID destinationWalletId,
            BigDecimal amount
    ) {
        boolean sameType = existingTransaction.getType() == TypeEnum.DEPOSIT;

        if (!sameType) {
            throw new IdempotencyConflictException();
        }

        UUID ownerId = existingTransaction.getDestinationWallet()
                .getUser()
                .getId();

        if (!authenticatedUserProvider.getUserId().equals(ownerId)) {
            throw new WalletAccessDeniedException(
                    existingTransaction.getDestinationWallet().getId()
            );
        }

        boolean sameDestination = existingTransaction.getDestinationWallet()
                .getId()
                .equals(destinationWalletId);

        boolean sameAmount = existingTransaction.getAmount()
                .compareTo(amount) == 0;

        if (!sameDestination || !sameAmount) {
            throw new IdempotencyConflictException();
        }

        return transactionMapper.toResponse(existingTransaction);
    }

    private TransactionDto.Response validateExistingTransfer(
            Transaction existingTransaction,
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount
    ) {
        if (existingTransaction.getType() != TypeEnum.TRANSFER) {
            throw new IdempotencyConflictException();
        }

        var ownerId = existingTransaction.getSourceWallet()
                .getUser()
                .getId();

        if (!authenticatedUserProvider.getUserId().equals(ownerId)) {
            throw new WalletAccessDeniedException(
                    existingTransaction.getSourceWallet().getId()
            );
        }

        boolean sameSource =
                existingTransaction.getSourceWallet()
                        .getId()
                        .equals(sourceWalletId);

        boolean sameDestination =
                existingTransaction.getDestinationWallet()
                        .getId()
                        .equals(destinationWalletId);

        boolean sameAmount =
                existingTransaction.getAmount()
                        .compareTo(amount) == 0;

        if (!sameSource || !sameDestination || !sameAmount) {
            throw new IdempotencyConflictException();
        }

        return transactionMapper.toResponse(existingTransaction);
    }
}
