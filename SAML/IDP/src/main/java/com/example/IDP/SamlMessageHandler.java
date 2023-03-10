package com.example.IDP;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.common.binding.encoding.SAMLMessageEncoder;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.ValidatorSuite;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.key.KeyManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.example.IDP.SamlBuilder.*;
import static java.util.Arrays.asList;
import static org.opensaml.xml.Configuration.getValidatorSuite;

public class SamlMessageHandler {
    private final KeyManager keyManager;
    private final SAMLMessageDecoder decoder;
    private final SecurityPolicyResolver resolver;
    private final List<ValidatorSuite> validatorSuites;
    private final SAMLMessageEncoder encoder;
    private final String entityId;

    public SamlMessageHandler(String entityId,
                              KeyManager keyManager,
                              SAMLMessageDecoder decoder,
                              SAMLMessageEncoder encoder,
                              SecurityPolicyResolver securityPolicyResolver) {
        this.entityId = entityId;
        this.keyManager = keyManager;
        this.decoder = decoder;
        this.encoder = encoder;
        this.resolver = securityPolicyResolver;
        this.validatorSuites = asList(getValidatorSuite("saml2-core-schema-validator")
                , getValidatorSuite("saml2-core-spec-validator"));
    }

    public SAMLMessageContext extractSAMLMessageContext(HttpServletRequest request, HttpServletResponse response)
            throws ValidationException, MessageDecodingException, SecurityException {
        SAMLMessageContext messageContext = new SAMLMessageContext();
        HttpServletRequestAdapter inTransport = new HttpServletRequestAdapter(request);
        HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(response, request.isSecure());
        request.setAttribute(org.springframework.security.saml.SAMLConstants.LOCAL_CONTEXT_PATH, request.getContextPath());
        messageContext.setInboundMessageTransport(inTransport);
        messageContext.setOutboundMessageTransport(outTransport);
        messageContext.setSecurityPolicyResolver(resolver);
        decoder.decode(messageContext);

        SAMLObject inboundSAMLMessage = messageContext.getInboundSAMLMessage();
        AuthnRequest authnRequest = (AuthnRequest) inboundSAMLMessage;
        for (ValidatorSuite validatorSuite : validatorSuites) {
            validatorSuite.validate(authnRequest);
        }
        return messageContext;
    }
    @SuppressWarnings("unchecked")
    public void sendAuthnResponse(SamlPrincipal principal, HttpServletResponse response)
            throws MarshallingException, SignatureException, MessageEncodingException {
        Status status = buildStatus(StatusCode.SUCCESS_URI);
        Credential signingCredential = resolveCredential(entityId);
        Response authResponse = buildSAMLObject(Response.class, Response.DEFAULT_ELEMENT_NAME);
        Issuer issuer = buildIssuer(entityId);
        authResponse.setIssuer(issuer);
        authResponse.setID(SamlBuilder.randomSAMLId());
        authResponse.setIssueInstant(new DateTime());
        authResponse.setInResponseTo(principal.getRequestID());

        Assertion assertion = buildAssertion(principal, status, entityId);
        signAssertion(assertion, signingCredential);
        authResponse.getAssertions().add(assertion);
        authResponse.setDestination(principal.getAssertionConsumerServiceUrl());
        authResponse.setStatus(status);

        Endpoint endpoint = buildSAMLObject(Endpoint.class, SingleSignOnService.DEFAULT_ELEMENT_NAME);
        endpoint.setLocation(principal.getAssertionConsumerServiceUrl());
        HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(response, false);

        @SuppressWarnings("rawtypes")
        BasicSAMLMessageContext messageContext = new BasicSAMLMessageContext();
        messageContext.setOutboundMessageTransport(outTransport);
        messageContext.setPeerEntityEndpoint(endpoint);
        messageContext.setOutboundSAMLMessage(authResponse);
        messageContext.setOutboundSAMLMessageSigningCredential(signingCredential);
        messageContext.setOutboundMessageIssuer(entityId);
        messageContext.setRelayState(principal.getRelayState());
        encoder.encode(messageContext);
    }

    private Credential resolveCredential(String entityId) {
        try {
            return keyManager.resolveSingle(new CriteriaSet(new EntityIDCriteria(entityId)));
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
