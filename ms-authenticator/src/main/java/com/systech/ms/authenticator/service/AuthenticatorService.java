package com.systech.ms.authenticator.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.systech.ms.authenticator.model.AuthenticationRequest;
import com.systech.ms.authenticator.model.AuthenticationResponse;
import com.systech.ms.authenticator.model.RefreshToken;
import com.systech.ms.authenticator.model.User;
import com.systech.ms.authenticator.repo.UserRepo;
import com.systech.ms.core.service.JwtService;

@Service
public class AuthenticatorService {
	@Autowired
	private MyUserDetailService myUserDetailService;
	@Autowired
	private JwtService jwtService;
	@Autowired
	private RefreshTokenService refreshService;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private UserRepo repo;
	@Autowired
	private PasswordEncoder passwordEncoder;


	public AuthenticationResponse login(AuthenticationRequest authenticationRequest) {
		User u=new User();
		
		if(isEmptyDatabase()) {
			List<String>auths=new ArrayList<String>();
			auths.add("admin");
			u.setUsername(authenticationRequest.getUsername());
			u.setPassword(passwordEncoder.encode(authenticationRequest.getPassword()));
			u.setEnabled(true);
			u.setAuths(auths);
			u.setLastAccess(new Date());
			repo.save(u);			
		}else {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					authenticationRequest.getUsername(), authenticationRequest.getPassword());
			authenticationManager.authenticate(authentication);
			u =(User) myUserDetailService.loadUserByUsername(authenticationRequest.getUsername());
			u.setLastAccess(new Date());
			repo.save(u);
		}
		return getAutenticationResponse(u);
	}
	
	public AuthenticationResponse refresh(String b64token) throws Exception {
		String token=new String(Base64.getUrlDecoder().decode(b64token.getBytes()));
		Optional<RefreshToken> rt=refreshService.findByToken(token);
		if(rt.isEmpty()) {
			throw new Exception ("Invalid refresh token");
		}
		
		refreshService.verifyExpiration(rt.get());
		UserDetails userDetails = myUserDetailService.loadUserByUsername(rt.get().getUser());
		return getAutenticationResponse(userDetails);
	}
	
	public String getApiKey(User u) throws Exception{
		if(u.getExpirationDate()==null) {
			throw new Exception ("ExpirationDate must be provided");
		}
		String token = jwtService.createAPIKey(u, u.getExpirationDate());
		
		u.setPassword(passwordEncoder.encode(u.getNewPassword()));
		u.setApiKey(token);
		repo.save(u);
		
		return token;
	}
	
	private AuthenticationResponse getAutenticationResponse(UserDetails userDetails) {
		String token = jwtService.createToken(userDetails);
		String refresh=refreshService.createRefreshToken(userDetails.getUsername()).getToken();
		return new AuthenticationResponse(token, Base64.getUrlEncoder().encodeToString(refresh.getBytes()));
	}
	private boolean isEmptyDatabase() {
		return repo.count()==0;
	}
}
