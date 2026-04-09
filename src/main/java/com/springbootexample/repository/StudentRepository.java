package com.springbootexample.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.springbootexample.entity.Student;
import com.springbootexample.entity.User;

public interface StudentRepository extends JpaRepository<Student,Long> {
	 Optional<User> findByEmail(String email);
	 List<Student> findByCourseIgnoreCase(String course);
	
	 Page<Student> findByCourseIgnoreCase(String course, Pageable pageable);
	 
	}
