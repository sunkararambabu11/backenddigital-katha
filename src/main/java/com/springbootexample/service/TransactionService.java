package com.springbootexample.service;

import java.util.List;
import java.util.Map;

import com.springbootexample.entity.Transaction;

public interface TransactionService {
	Transaction addTransaction(Transaction request, Long id);
	List<Transaction> getCustomerTransactions(Long customerId, Long userId);
	void deleteTransaction(Long id, Long userId);
	Transaction updateTransaction(Long id, Transaction request, Long userId);
	void createFromAi(Map<String, Object> data, String userId);
	
}
