package com.springbootexample.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.springbootexample.entity.Customer;
import com.springbootexample.repository.CustomerRepository;

@Service
public class CustomerImpl implements CustomerService {

    @Autowired
    private CustomerRepository repository;

    // ✅ CREATE CUSTOMER
    @Override
    public Customer create(Customer request, Long userId) {

        repository.findByMobile(request.getMobile())
                .ifPresent(c -> {
                    throw new RuntimeException("Mobile already exists");
                });

        if (request.getOpeningBalance() == null) {
            request.setOpeningBalance(0.0);
        }

        request.setCurrentBalance(request.getOpeningBalance());
        request.setUserId(userId); // 🔥 important

        return repository.save(request);
    }

    // ✅ GET ALL (JWT BASED)
    @Override
    public List<Customer> getAllcustomersData(Long userId) {
        return repository.findByUserId(userId);
    }

    // ✅ GET BY ID (SECURE 🔥)
    @Override
    public Customer getCustomerById(Long id, Long userId) {

        Customer customer = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!customer.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access ❌");
        }

        return customer;
    }

    // ✅ UPDATE
    @Override
    public Customer updateCustomer(Long id, Customer request, Long userId) {

        Customer customer = getCustomerById(id, userId); // 🔥 reuse

        customer.setName(request.getName());
        customer.setMobile(request.getMobile());
       customer.setDescription(request.getDescription());
        return repository.save(customer);
    }

    // ✅ DELETE
    @Override
    public void deletecustomer(Long id, Long userId) {

        Customer customer = getCustomerById(id, userId);
        repository.delete(customer);
    }

	@Override
	public Customer saveCustomer(Customer customer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Customer> getAllcustomers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Customer getCustomerById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Customer> getAllcustomersData1(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}
}