package com.example.OAuth;


import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;



@Component
@Singleton
public class Database {

    private Set<String> authCodes = new HashSet<>();
    private Set<String> tokens = new HashSet<>();

    public void addAuthCode(String authCode) {
        authCodes.add(authCode);
    }

    public boolean isValidAuthCode(String authCode) {
        return authCodes.contains(authCode);
    }

    public void addToken(String token) {
        tokens.add(token);
    }

    public boolean isValidToken(String token) {
        return tokens.contains(token);
    }
}