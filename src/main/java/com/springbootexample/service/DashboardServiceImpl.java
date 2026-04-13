package com.springbootexample.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.springbootexample.entity.Transaction;
import com.springbootexample.entity.Customer; 
import com.springbootexample.dto.DashboardDTO;
import com.springbootexample.dto.RecentTransactionDTO;
import com.springbootexample.dto.TopDebtorDTO;
import com.springbootexample.repository.CustomerRepository;
import com.springbootexample.repository.TransactionRepository;

@Service
public class DashboardServiceImpl implements DashboardService {
	@Autowired
	private CustomerRepository customerRepo;
	@Autowired
	private TransactionRepository txnRepo;
	
	@Override
	public DashboardDTO getDashboard(Long userId) {
		
		  List<Customer> customers = customerRepo.findByUserId(userId);
	        //List<Transaction> txns = txnRepo.findAll();
	        List<Transaction> txns = txnRepo.findByUserId(userId);
	        

	        double debit = 0;
	        double credit = 0;

	        for (Transaction t : txns) {

	           // if (!t.getUserId().equals(userId)) continue;

	            if ("DEBIT".equalsIgnoreCase(t.getType())) {
	                debit += t.getAmount();
	            } else {
	                credit += t.getAmount();
	            }
	        }

	        DashboardDTO dto = new DashboardDTO();
	        dto.setTotalCustomers(customers.size());
	        dto.setTotalDebit(debit);
	        dto.setTotalCredit(credit);
	        dto.setTotalOutstanding(debit - credit);

	        return dto;
			}
	@Override
	public List<TopDebtorDTO> getTopDebtors(Long userId) {

	    return customerRepo.findByUserId(userId).stream()
	            .sorted((a, b) -> Double.compare(
	                    b.getCurrentBalance(),
	                    a.getCurrentBalance()))
	            .limit(5)
	            .map(c -> {
	                TopDebtorDTO dto = new TopDebtorDTO();
	                dto.setName(c.getName());
	                dto.setBalance(c.getCurrentBalance());
	                return dto;
	            })
	            .toList();
	}

	@Override
	public List<RecentTransactionDTO> getRecentTransactions(Long userId) {

	    return txnRepo.findTop5ByUserIdOrderByCreatedAtDesc(userId)
	            .stream()
	            .map(t -> {
	                RecentTransactionDTO dto = new RecentTransactionDTO();
	                dto.setType(t.getType());
	                dto.setAmount(t.getAmount());
	                dto.setName("Customer-" + t.getCustomerId());
	                return dto;
	            })
	            .toList();
	}
	@Override
	public Map<String, Double> getMonthlyReport(Long userId) {

	    List<Transaction> txns = txnRepo.findAll();

	    Map<String, Double> map = new HashMap<>();

	    for (Transaction t : txns) {

	        if (!t.getUserId().equals(userId)) continue;

	        String month = t.getCreatedAt().getMonth().toString();

	        map.put(month,
	                map.getOrDefault(month, 0.0) + t.getAmount());
	    }

	    return map;
	}
	public Map<String, Object> getSummary(Long userId) {

	    Map<String, Object> data = new HashMap<>();

	    data.put("totalCustomers", 10);       // replace with real logic
	    data.put("totalOutstanding", 5000);   // replace with real logic

	    return data;
	}
}
