package com.systech.ms.authenticator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.systech.ms.authenticator.repo.UserRepo;

@Service
public class MyUserDetailService implements UserDetailsService {
	@Autowired
	private UserRepo repo;
	
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    	UserDetails u=repo.findByUsername(username);
    	if(u==null) {
    		throw new UsernameNotFoundException(username);
    	}
		return u;
    }

}
