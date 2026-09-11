package com.systech.ms.core.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.systech.ms.core.constants.JwtTypes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class JwtService {
	private static int EXPIRATION_TIME;
	private static final String AUTHORITIES = "authorities";
	private static final String JWTTYPE="jwtType";
	private static String SECRET_KEY;
	
	
	public JwtService(@Value("${jwtgenerator.secret-key}") String key,
			@Value("${jwtgenerator.expiration-time:#{60000}}") int et) {
		SECRET_KEY = Base64.getEncoder().encodeToString(key.getBytes());
		EXPIRATION_TIME = et;
	}

	public String createToken(UserDetails userDetails) {

		String username = userDetails.getUsername();
		Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
		return Jwts.builder().setSubject(username)
				.claim(AUTHORITIES, authorities)
				.claim(JWTTYPE, JwtTypes.TOKEN)
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
				.signWith(SignatureAlgorithm.HS512, SECRET_KEY).compact();
	}

	public String createAPIKey(UserDetails userDetails, Date expirationDate) {

		String username = userDetails.getUsername();
		Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
		return Jwts.builder().setSubject(username)
				.claim(AUTHORITIES, authorities)
				.claim(JWTTYPE, JwtTypes.APIKEY)
				.setExpiration(expirationDate)
				.signWith(SignatureAlgorithm.HS512, SECRET_KEY).compact();
	}

	public Boolean hasTokenExpired(String token) {
		return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody().getExpiration()
				.before(new Date());
	}

	public Boolean validateToken(String token) {
		return (!hasTokenExpired(token));

	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return (userDetails.getUsername().equals(username) && !hasTokenExpired(token));

	}

	public UserDetails getUserDetails(String token) {
		Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
		UserDetails u = new User(claims.getSubject(), "", getAuthorities(token));
		return u;
	}

	public String extractUsername(String token) {
		return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody().getSubject();
	}

	public List<String> getAuthoritiesList(Claims claims) {
		ArrayList<LinkedHashMap> aus = (ArrayList) claims.get(AUTHORITIES);
		List<String> ret = new ArrayList();
		aus.stream().forEach(a -> ret.add((String) a.get("authority")));
		return ret;
	}

	public Collection<? extends GrantedAuthority> getAuthorities(String token) {
		Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
		ArrayList aus = (ArrayList) claims.get(AUTHORITIES);
		Iterator it = aus.iterator();
		Set s = new HashSet();
		while (it.hasNext()) {
			LinkedHashMap lhm = (LinkedHashMap) it.next();
			s.add(new SimpleGrantedAuthority((String) lhm.get("authority")));
		}

		return (Collection<? extends GrantedAuthority>) s;
	}
}
