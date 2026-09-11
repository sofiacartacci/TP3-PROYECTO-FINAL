package com.systech.ms.authenticator.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import com.systech.ms.authenticator.model.User;

@Repository
public interface UserRepo extends MongoRepository<User,String> {
	public Optional<User> findById(String user);
	
	public UserDetails findByUsername(String user);
	public List<User>findAll();
	public User save(User user);
}
