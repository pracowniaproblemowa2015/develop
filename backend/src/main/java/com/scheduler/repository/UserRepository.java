package com.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scheduler.model.User;


public interface UserRepository  extends JpaRepository<User, Long> {
	
	User findOneByLogin(String login); 
	
}