package com.springbootexample.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.springbootexample.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
List<Transaction> findBycustomerId(Long customerId);
List<Transaction> findByUserId(Long userId);
List<Transaction> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
@Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId AND t.createdAt BETWEEN :from AND :to")
List<Transaction> findByCustomerAndDateRange(
        Long customerId,
        LocalDateTime from,
        LocalDateTime to
);
}
