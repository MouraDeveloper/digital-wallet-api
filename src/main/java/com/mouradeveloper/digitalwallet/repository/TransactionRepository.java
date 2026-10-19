package com.mouradeveloper.digitalwallet.repository;

import com.mouradeveloper.digitalwallet.models.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
