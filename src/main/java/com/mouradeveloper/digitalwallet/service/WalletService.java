package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.forbidden.WalletAccessDeniedException;
import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.mapper.WalletMapper;
import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import com.mouradeveloper.digitalwallet.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;


    @Transactional(readOnly = true)
    public WalletDto.Response findById(UUID walletId) {
        Wallet wallet = findWalletOrThrow(walletId);

        if (!wallet.getUser().getId().equals(authenticatedUserProvider.getUserId())) {
            throw new WalletAccessDeniedException(walletId);
        }

        return walletMapper.toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletDto.Response findMyWallet() {
        UUID userId = authenticatedUserProvider.getUserId();

        return walletRepository.findByUser_Id(userId)
                .map(walletMapper::toResponse)
                .orElseThrow(WalletNotFoundException::new);
    }


    private Wallet findWalletOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(
                        () -> new WalletNotFoundException(walletId)
                );
    }
}
