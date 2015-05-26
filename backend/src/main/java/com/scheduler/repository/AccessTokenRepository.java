package com.scheduler.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scheduler.model.AccessToken;
import com.scheduler.model.User;


public interface AccessTokenRepository  extends JpaRepository<AccessToken, Long> {

	List<AccessToken> findByUserOrderByExpireDesc(User user);
	
	AccessToken findByToken(String token);
	
	List<AccessToken> findByUser(User user);
}