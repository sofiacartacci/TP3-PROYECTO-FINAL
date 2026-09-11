package com.systech.ms.authenticator.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
@Document(collection = "users")
public class User implements UserDetails {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	private String username;
	@JsonIgnore
	private String password;
	@Transient
	private String newPassword;
	private boolean enabled;
	private String token;
	private Date lastAccess;
	private Date expirationDate;
	private String apiKey;
	private List<String> auths;

	
	public void setAuths(List<String> auths) {
		this.auths = auths;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	
	public List<String> getAuths() {
		return auths;
	}
	
	@Override
	@JsonIgnore
	public List<GrantedAuthority> getAuthorities() {	
		if(auths==null) {
			return null;
		}else {
			String[] as= new String[auths.size()]; 
			as=auths.toArray(as); 
			List<GrantedAuthority> ret=AuthorityUtils.createAuthorityList(as);
			return ret;
		}
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	

}
