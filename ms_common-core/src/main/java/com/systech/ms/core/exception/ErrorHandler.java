package com.systech.ms.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

@RestControllerAdvice
public class ErrorHandler {
	Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ControllerError handleAccessDenied(Exception e) {
        return new ControllerError("Access Denied. Endpoint requires an authority that the user does not have",e);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ControllerError handleAuth(Exception e) {
        return new ControllerError(e.getMessage());
    }
    
    @ExceptionHandler({MalformedJwtException.class, ExpiredJwtException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ControllerError handlejwt(Exception e) {
        return new ControllerError(e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ControllerError handleGeneric(Exception e) {
    	logger.error(e.getMessage(),e);
        return new ControllerError(e.getMessage(),e);
    }
    
}