package com.systech.ms.authenticator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.systech.ms.authenticator.service.MyUserDetailService;

@Component
public class AuthenticatorConfig {
	@Autowired
	private MyUserDetailService myUserDetailService;
	public void configure(AuthenticationManagerBuilder builder) throws Exception {
        builder.userDetailsService(myUserDetailService).passwordEncoder(new BCryptPasswordEncoder());; 
    }
}
