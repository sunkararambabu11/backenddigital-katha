package com.springbootexample.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.springbootexample.entity.Transaction;
import com.springbootexample.entity.Customer;
import com.springbootexample.dto.DashboardDTO;
import com.springbootexample.dto.DashboardSummaryDTO;
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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public DashboardDTO getDashboard(Long userId) {
        List<Customer> customers = customerRepo.findByUserId(userId);
        List<Transaction> txns = txnRepo.findByUserId(userId);

        double debit = 0;
        double credit = 0;

        for (Transaction t : txns) {
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
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .sorted((a, b) -> Double.compare(b.getCurrentBalance(), a.getCurrentBalance()))
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
        // Build customerId -> customerName map for resolving names
        Map<Long, String> customerNames = customerRepo.findByUserId(userId).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));

        return txnRepo.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(t -> {
                    RecentTransactionDTO dto = new RecentTransactionDTO();
                    dto.setType(t.getType());
                    dto.setAmount(t.getAmount());
                    dto.setDescription(t.getDescription());
                    // Resolve real customer name
                    String name = customerNames.get(t.getCustomerId());
                    dto.setName(name != null ? name : "Unknown");
                    // Format date
                    if (t.getCreatedAt() != null) {
                        dto.setDate(t.getCreatedAt().format(DATE_FMT));
                    }
                    return dto;
                })
                .toList();
    }

    @Override
    public Map<String, Double> getMonthlyReport(Long userId) {
        List<Transaction> txns = txnRepo.findByUserId(userId);
        Map<String, Double> map = new HashMap<>();

        for (Transaction t : txns) {
            if (t.getCreatedAt() == null) continue;
            String month = t.getCreatedAt().getMonth().toString();
            map.put(month, map.getOrDefault(month, 0.0) + t.getAmount());
        }
        return map;
    }

    @Override
    public DashboardSummaryDTO getFullSummary(Long userId) {
        List<Customer> customers = customerRepo.findByUserId(userId);
        List<Transaction> txns = txnRepo.findByUserId(userId);

        // Build customer name map
        Map<Long, String> customerNames = customers.stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));

        // Core stats
        double totalDebit = 0;
        double totalCredit = 0;
        long todayTxnCount = 0;
        double todayDebit = 0;
        double todayCredit = 0;
        LocalDate today = LocalDate.now();

        for (Transaction t : txns) {
            boolean isDebit = "DEBIT".equalsIgnoreCase(t.getType());
            double amount = t.getAmount() != null ? t.getAmount() : 0;

            if (isDebit) {
                totalDebit += amount;
            } else {
                totalCredit += amount;
            }

            // Today's stats
            if (t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(today)) {
                todayTxnCount++;
                if (isDebit) {
                    todayDebit += amount;
                } else {
                    todayCredit += amount;
                }
            }
        }

        long activeCustomers = customers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .count();

        // Top debtors
        List<TopDebtorDTO> topDebtors = customers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance() > 0)
                .sorted((a, b) -> Double.compare(b.getCurrentBalance(), a.getCurrentBalance()))
                .limit(5)
                .map(c -> {
                    TopDebtorDTO dto = new TopDebtorDTO();
                    dto.setName(c.getName());
                    dto.setBalance(c.getCurrentBalance());
                    return dto;
                })
                .toList();

        // Recent transactions (top 10 with real names)
        List<RecentTransactionDTO> recent = txnRepo.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(t -> {
                    RecentTransactionDTO dto = new RecentTransactionDTO();
                    dto.setType(t.getType());
                    dto.setAmount(t.getAmount());
                    dto.setDescription(t.getDescription());
                    String name = customerNames.get(t.getCustomerId());
                    dto.setName(name != null ? name : "Unknown");
                    if (t.getCreatedAt() != null) {
                        dto.setDate(t.getCreatedAt().format(DATE_FMT));
                    }
                    return dto;
                })
                .toList();

        // Monthly data
        Map<String, Double> monthlyData = new HashMap<>();
        for (Transaction t : txns) {
            if (t.getCreatedAt() == null) continue;
            String month = t.getCreatedAt().getMonth().toString();
            monthlyData.put(month, monthlyData.getOrDefault(month, 0.0) + t.getAmount());
        }

        // Build response
        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setTotalCustomers(customers.size());
        summary.setActiveCustomers(activeCustomers);
        summary.setTotalDebit(totalDebit);
        summary.setTotalCredit(totalCredit);
        summary.setTotalOutstanding(totalDebit - totalCredit);
        summary.setTodayTransactionCount(todayTxnCount);
        summary.setTodayDebit(todayDebit);
        summary.setTodayCredit(todayCredit);
        summary.setTopDebtors(topDebtors);
        summary.setRecentTransactions(recent);
        summary.setMonthlyData(monthlyData);

        return summary;
    }

    @Override
    public Map<String, Object> getSummary(Long userId) {
        // Legacy method used by AiService — now delegate to real data
        DashboardSummaryDTO full = getFullSummary(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("totalCustomers", full.getTotalCustomers());
        data.put("totalOutstanding", full.getTotalOutstanding());
        data.put("totalDebit", full.getTotalDebit());
        data.put("totalCredit", full.getTotalCredit());
        data.put("activeCustomers", full.getActiveCustomers());
        return data;
    }
}
