package com.springbootexample.service;

import java.util.List;
import java.util.Map;

import com.springbootexample.entity.Customer;

public interface CustomerService {
 
	public Customer saveCustomer(Customer customer);
	
	List<Customer> getAllcustomers();
	
	List<Customer> getAllcustomersData(Long userId);
	
	Customer getCustomerById(Long id);

	Customer create(Customer request, Long userId);

	Customer getCustomerById(Long id, Long userId);

	List<Customer> getAllcustomersData1(Long userId);
	
	void createFromAi(Map<String, Object> data, String userId);

	void deletecustomer(Long id, Long userId);

	Customer updateCustomer(Long id, Customer request, Long userId);
}
