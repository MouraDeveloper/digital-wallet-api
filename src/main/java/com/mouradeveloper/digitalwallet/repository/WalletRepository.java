package com.mouradeveloper.digitalwallet.repository;

import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
