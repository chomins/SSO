package com.example.IDP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.util.Objects.isNull;

public class SamlResponseFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdpApplication.class);

    private final String ssoUrl;
    private SamlMessageHandler samlMessageHandler;
    private AbstractSamlPrincipalFactory samlPrincipalFactory;

    public SamlResponseFilter(String ssoUrl) {
        this.ssoUrl = ssoUrl;
    }

    @Autowired
    public void setSamlMessageHandler(SamlMessageHandler samlMessageHandler) {
        this.samlMessageHandler = samlMessageHandler;
    }

    @Autowired
    public void setSamlMessageHandler(AbstractSamlPrincipalFactory samlPrincipalFactory) {
        this.samlPrincipalFactory = samlPrincipalFactory;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!request.getRequestURI().startsWith(ssoUrl)) {
                filterChain.doFilter(request, response);
                return;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (isNull(authentication)
                    || authentication instanceof AnonymousAuthenticationToken
                    || !authentication.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }
            @SuppressWarnings("rawtypes")
            SAMLMessageContext messageContext = samlMessageHandler.extractSAMLMessageContext(request, response);
            SamlPrincipal principal = samlPrincipalFactory.createSamlPrincipal(messageContext, authentication);
            samlMessageHandler.sendAuthnResponse(principal, response);
        } catch (Exception e) {
            throw new ServletException("Failed to send saml response.", e);
        }
    }
}
