package com.springbootexample.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springbootexample.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	
	 Optional<Customer> findByMobile(String mobile);
	 List<Customer> findByUserId(Long userId);
	 Optional<Customer> findByName(String name);
	 

}
