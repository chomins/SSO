package com.example.IDP;

import org.opensaml.common.binding.security.IssueInstantRule;
import org.opensaml.saml2.binding.decoding.HTTPRedirectDeflateDecoder;
import org.opensaml.saml2.binding.encoding.HTTPPostSimpleSignEncoder;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.ws.security.provider.BasicSecurityPolicy;
import org.opensaml.ws.security.provider.StaticSecurityPolicyResolver;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

import javax.servlet.SessionCookieConfig;
import java.security.KeyStore;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigure {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${idp.sso_url}")
    private String ssoUrl;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/error").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login").permitAll()
                .failureUrl("/login?error=true").permitAll()
                .and()
                .logout()
                .logoutSuccessUrl("/login")
                .and()
                .addFilterAfter(samlResponseFilter(), FilterSecurityInterceptor.class)
                .csrf().disable();

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return (web ->   web.ignoring().antMatchers("/favicon.ico", "/*.js"));
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("user")
                .password("user123")
                .roles("USER")
                .build();
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin123")
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
            sessionCookieConfig.setName("idp.session");
            sessionCookieConfig.setHttpOnly(true);
        };
    }

    @Bean
    public SamlResponseFilter samlResponseFilter() {
        return new SamlResponseFilter(ssoUrl);
    }

    @Bean
    public JKSKeyManager keyManager(@Value("${idp.entity_id}") String idpEntityId,
                                    @Value("${idp.private_key}") String idpPrivateKey,
                                    @Value("${idp.certificate}") String idpCertificate,
                                    @Value("${idp.passphrase}") String idpPassphrase) throws Exception {
        KeyStore keyStore = KeyStoreLocator.createKeyStore(idpPassphrase);
        KeyStoreLocator.addPrivateKey(keyStore, idpEntityId, idpPrivateKey, idpCertificate, idpPassphrase);
        return new JKSKeyManager(keyStore, Collections.singletonMap(idpEntityId, idpPassphrase), idpEntityId);
    }

    @Bean
    public LocalSamlPrincipalFactory samlPrincipalFactory() {
        return new LocalSamlPrincipalFactory(NameIDType.UNSPECIFIED);
    }

    @Bean
    public SamlMessageHandler samlMessageHandler(@Value("${idp.entity_id}") String idpEntityId,
                                                 @Value("${idp.clock_skew}") int clockSkew,
                                                 @Value("${idp.expires}") int expires,
                                                 JKSKeyManager keyManager) throws XMLParserException {
        StaticBasicParserPool parserPool = new StaticBasicParserPool();
        parserPool.initialize();
        BasicSecurityPolicy securityPolicy = new BasicSecurityPolicy();
        securityPolicy.getPolicyRules().addAll(Collections.singletonList(new IssueInstantRule(clockSkew, expires)));

        HTTPRedirectDeflateDecoder httpRedirectDeflateDecoder = new HTTPRedirectDeflateDecoder(parserPool);
        HTTPPostSimpleSignEncoder httpPostSimpleSignEncoder = new HTTPPostSimpleSignEncoder(
                VelocityFactory.getEngine(),
                "/templates/saml2-post-binding.vm",
                true
        );

        return new SamlMessageHandler(
                idpEntityId,
                keyManager,
                httpRedirectDeflateDecoder,
                httpPostSimpleSignEncoder,
                new StaticSecurityPolicyResolver(securityPolicy)
        );
    }
}
