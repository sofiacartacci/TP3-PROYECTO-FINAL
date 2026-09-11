package com.systech.ms.authenticator.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.systech.ms.authenticator.model.RefreshToken;
import com.systech.ms.authenticator.repo.RefreshTokenRepo;

@Service
public class RefreshTokenService {
  @Value("${jwtgenerator.refresh-time}")
  private Long refreshTokenDurationMs;

  @Autowired
  private RefreshTokenRepo refreshTokenRepository;

  
  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  public RefreshToken createRefreshToken(String userId) {
    RefreshToken refreshToken = new RefreshToken();

    refreshToken.setUser(userId);
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
    refreshToken.setToken(UUID.randomUUID().toString());

    refreshToken = refreshTokenRepository.save(refreshToken);
    return refreshToken;
  }

  public RefreshToken verifyExpiration(RefreshToken token) throws Exception {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      throw new Exception("Refresh token has expired. Please login again");
    }
    return token;
  }

  @Transactional
  public int deleteByUserId(String userId) {
    return refreshTokenRepository.deleteByUser(userId);
  }
}
