package com.systech.ms.authenticator.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "refreshtoken")
public class RefreshToken {
		  @Id
		  private String user;

		  private String token;

		  private Instant expiryDate;

		  public RefreshToken() {
		  }

		  public String getUser() {
		    return user;
		  }

		  public void setUser(String user) {
		    this.user = user;
		  }

		  public String getToken() {
		    return token;
		  }

		  public void setToken(String token) {
		    this.token = token;
		  }

		  public Instant getExpiryDate() {
		    return expiryDate;
		  }

		  public void setExpiryDate(Instant expiryDate) {
		    this.expiryDate = expiryDate;
		  }
}
