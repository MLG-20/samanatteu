package com.samanatteu.exception.auth;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class RefreshTokenInvalideException extends SamanatteuException {
    public RefreshTokenInvalideException(){
        super("Refresh token invalide ou expiré", HttpStatus.UNAUTHORIZED);
    }
}
