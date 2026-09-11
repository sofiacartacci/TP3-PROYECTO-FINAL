package com.systech.ms.authenticator.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.systech.ms.authenticator.model.AuthenticationRequest;
import com.systech.ms.authenticator.model.AuthenticationResponse;
import com.systech.ms.authenticator.model.User;
import com.systech.ms.authenticator.repo.UserRepo;
import com.systech.ms.authenticator.service.AuthenticatorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {

	Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private AuthenticatorService service;

	@Autowired
	private UserRepo repo;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Operation(summary = "Autenticar usuario y obtener jwt y refresh token")
	@PostMapping("/login")
	public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest authenticationRequest) {
		return ResponseEntity.ok(service.login(authenticationRequest));		
	}
	
	@Operation(summary = "Obtener nuevo jwt y refresh token a partir de un refresh token")
	@GetMapping("/refresh/{refresh}")
	public ResponseEntity<AuthenticationResponse> refres(@PathVariable String refresh) throws Exception{
		return ResponseEntity.ok(service.refresh(refresh));		
	}

	@Operation(summary = "Listar usuarios", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/list")
	public ResponseEntity<List<User>> list() {
		List<User> l = repo.findAll();
		return ResponseEntity.ok(l);
	}

	@Operation(summary = "Obtener datos de un usuario por su username", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/get/{u}")
	public ResponseEntity<Optional<User>> get(@PathVariable String u) {
		Optional<User> user = repo.findById(u);
		if (user.isEmpty()) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(user);
		}
	}

	@Operation(summary = "Dar de alta o modificar un usuario", security = @SecurityRequirement(name = "bearerAuth"))
	@PreAuthorize("hasAuthority('admin')")
	@PostMapping("/save")
	public ResponseEntity<User> save(@RequestBody User user) {
		user.setPassword(passwordEncoder.encode(user.getNewPassword()));
		User u = repo.save(user);
		return ResponseEntity.ok(u);
	}
	
	@Operation(summary = "Obtener una APIKey para consumo m2m", security = @SecurityRequirement(name = "bearerAuth"))
	@PreAuthorize("hasAuthority('admin')")
	@PostMapping("/getAPIKey")
	public ResponseEntity<String> getAPIKey(@RequestBody User user) throws Exception{		
		return ResponseEntity.ok(service.getApiKey(user));
	}

	@Operation(summary = "Eliminar un usurio por su username", security = @SecurityRequirement(name = "bearerAuth"))
	@PreAuthorize("hasAuthority('admin')")
	@DeleteMapping("/delete/{u}")
	public ResponseEntity<Optional<User>> delete(@PathVariable String u) {
		Optional<User> user = repo.findById(u);
		if (user.isEmpty()) {
			return ResponseEntity.notFound().build();
		} else {
			repo.delete(user.get());
			return ResponseEntity.ok(user);
		}
	}
}