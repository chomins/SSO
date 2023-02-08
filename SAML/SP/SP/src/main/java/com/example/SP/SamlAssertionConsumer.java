package com.example.SP;

import org.opensaml.saml2.core.Response;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

public interface SamlAssertionConsumer {
    UserDetails consume(Response samlResponse) throws AuthenticationException;
}
