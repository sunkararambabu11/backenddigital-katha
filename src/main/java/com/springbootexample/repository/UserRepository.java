package com.springbootexample.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springbootexample.entity.User;

public interface UserRepository extends JpaRepository<User,Long> {

//	Optional<User> findByEmail(String email);
//	Optional<User> findByMobileOrEmail(String mobile, String email);
//	Optional<User> findByMobile(String mobile);
//	List<User>  findByUserId(String userid);
//	
	Optional<User> findByEmail(String email);

    Optional<User> findByMobileOrEmail(String mobile, String email);

    Optional<User> findByMobile(String mobile);


}
