package com.systech.ms.core;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systech.ms.core.exception.ControllerError;
import com.systech.ms.core.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

	private static final String HEADER_TOKEN_PREFIX = "Bearer ";
	private static final String HEADER_AUTHORIZATION = "Authorization";

	
	@Autowired
	private JwtService jwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);
		if (authorizationHeader != null && authorizationHeader.startsWith(HEADER_TOKEN_PREFIX)) {
			
			String token = authorizationHeader.replace(HEADER_TOKEN_PREFIX, "");
			try {
				if (jwtService.validateToken(token)) {
					UserDetails userDetails=jwtService.getUserDetails(token);
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
			catch(ExpiredJwtException | SignatureException e) {
				ObjectMapper mapper = new ObjectMapper();
				ControllerError ce=new ControllerError(e.getMessage());
			    response.setContentType("application/json");
			    response.setStatus(HttpServletResponse.SC_FORBIDDEN);  
			    response.getWriter().write(mapper.writeValueAsString(ce));
			    return;
			}
			catch(UsernameNotFoundException e) {
				ObjectMapper mapper = new ObjectMapper();
				ControllerError ce=new ControllerError("User name " + e.getMessage() + " not found");
			    response.setContentType("application/json");
			    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  
			    response.getWriter().write(mapper.writeValueAsString(ce));
			    return;
			}
			catch(Exception e) {
				ObjectMapper mapper = new ObjectMapper();
				ControllerError ce=new ControllerError(e.getMessage(),e);
			    response.setContentType("application/json");
			    response.setStatus(HttpServletResponse.SC_FORBIDDEN);  
			    response.getWriter().write(mapper.writeValueAsString(ce));
			    return;
			}
		}
		filterChain.doFilter(request, response);
	}
}
