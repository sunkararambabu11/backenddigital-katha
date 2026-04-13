package com.springbootexample.service;

import java.util.List;
import java.util.Map;

import com.springbootexample.entity.Customer;
import com.springbootexample.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.springbootexample.repository.CustomerRepository;
import com.springbootexample.repository.TransactionRepository;
@Service
public class TransactionServiceImpl implements TransactionService {
	@Autowired
    private CustomerRepository customerRepo;
	@Autowired
    private TransactionRepository txnRepo;

	@Override
	public Transaction addTransaction(Transaction request, Long userId) {
		Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found")); 
		if (!customer.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized ❌");
        }
		
		 Double balance = customer.getCurrentBalance();
		   if ("DEBIT".equalsIgnoreCase(request.getType())) {
	            balance += request.getAmount();
	        } else if ("CREDIT".equalsIgnoreCase(request.getType())) {
	            balance -= request.getAmount();
	        } else {
	            throw new RuntimeException("Invalid type");
	        }

	        customer.setCurrentBalance(balance);
	        customerRepo.save(customer);

	        request.setUserId(userId);
	        request.setBalanceAfter(balance);

	        return txnRepo.save(request);
	        }

	@Override
	public List<Transaction> getCustomerTransactions(Long customerId, Long userId) {
		Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!customer.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized ❌");
        }

        return txnRepo.findBycustomerId(customerId);
    }
	
	@Override
	public void deleteTransaction(Long id, Long userId) {

	    Transaction txn = txnRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Transaction not found"));

	    Customer customer = customerRepo.findById(txn.getCustomerId())
	            .orElseThrow(() -> new RuntimeException("Customer not found"));

	    // 🔐 Security
	    if (!customer.getUserId().equals(userId)) {
	        throw new RuntimeException("Unauthorized ❌");
	    }

	    Double balance = customer.getCurrentBalance();

	    // 🔥 Reverse logic
	    if ("DEBIT".equalsIgnoreCase(txn.getType())) {
	        balance -= txn.getAmount();
	    } else if ("CREDIT".equalsIgnoreCase(txn.getType())) {
	        balance += txn.getAmount();
	    }

	    customer.setCurrentBalance(balance);
	    customerRepo.save(customer);

	    txnRepo.delete(txn);
	}
	public Transaction updateTransaction(Long id, Transaction request, Long userId) {

	    Transaction oldTxn = txnRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Transaction not found"));

	    Customer customer = customerRepo.findById(oldTxn.getCustomerId())
	            .orElseThrow(() -> new RuntimeException("Customer not found"));

	    // 🔐 Security
	    if (!customer.getUserId().equals(userId)) {
	        throw new RuntimeException("Unauthorized ❌");
	    }

	    Double balance = customer.getCurrentBalance();

	    // 🔥 STEP 1: Reverse old transaction
	    if ("DEBIT".equalsIgnoreCase(oldTxn.getType())) {
	        balance -= oldTxn.getAmount();
	    } else if ("CREDIT".equalsIgnoreCase(oldTxn.getType())) {
	        balance += oldTxn.getAmount();
	    }

	    // 🔥 STEP 2: Apply new transaction
	    if ("DEBIT".equalsIgnoreCase(request.getType())) {
	        balance += request.getAmount();
	    } else if ("CREDIT".equalsIgnoreCase(request.getType())) {
	        balance -= request.getAmount();
	    } else {
	        throw new RuntimeException("Invalid type");
	    }

	    // 🔥 Update customer balance
	    customer.setCurrentBalance(balance);
	    customerRepo.save(customer);

	    // 🔥 Update transaction
	    oldTxn.setType(request.getType());
	    oldTxn.setAmount(request.getAmount());
	    oldTxn.setDescription(request.getDescription());
	    oldTxn.setBalanceAfter(balance);

	    return txnRepo.save(oldTxn);
	}
	public void createFromAi(Map<String, Object> data, String userId) {

	    Customer customer = txnRepo
	        .findByName(data.get("name").toString())
	        .orElseThrow(() -> new RuntimeException("Customer not found"));

	    Transaction t = new Transaction();

	    t.setCustomerId(customer.getId());
	    t.setAmount(Double.parseDouble(data.get("amount").toString()));
	    t.setType(data.get("type").toString());
	    t.setDescription(data.get("description").toString());

	    txnRepo.save(t);
	}
	}

