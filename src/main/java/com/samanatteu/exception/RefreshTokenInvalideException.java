package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenInvalideException extends SamanatteuException {
    public RefreshTokenInvalideException(){
        super("Refresh token invalide ou expiré", HttpStatus.UNAUTHORIZED);
    }
}
