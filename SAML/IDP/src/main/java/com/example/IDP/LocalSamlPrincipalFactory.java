package com.example.IDP;

import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

public class LocalSamlPrincipalFactory extends AbstractSamlPrincipalFactory{

    public LocalSamlPrincipalFactory(String nameIdType) {
        super(nameIdType);
    }
    @Override
    protected List<SamlAttribute> createAttributes(Authentication authentication) {
        return Arrays.asList(
                new SamlAttribute("User.Username", authentication.getName()),
                new SamlAttribute("User.Email", "test@test.com"),
                new SamlAttribute("User.FederationIdentifier", "test@test.com")
        );
    }
}
