package com.systech.ms.authenticator.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.systech.ms.authenticator.model.RefreshToken;

@Repository
public interface RefreshTokenRepo extends MongoRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByToken(String token);
	
	int deleteByUser(String user);
}
