package com.example.cognito_lab.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

@Component
public class CognitoJwtValidator {

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    @Value("${cognito.region}")
    private String region;

    @Value("${cognito.client-id}")
    private String clientId;

    private JwkProvider jwkProvider;

    @PostConstruct
    public void init() throws MalformedURLException {
        String jwksUrl = String.format(
                "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                region, userPoolId
        );

        this.jwkProvider = new JwkProviderBuilder(new URL(jwksUrl))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();
    }

    public DecodedJWT validate(String token) throws Exception {
        DecodedJWT decoded = JWT.decode(token);

        Jwk jwk = jwkProvider.get(decoded.getKeyId());
        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

        String issuer = String.format(
                "https://cognito-idp.%s.amazonaws.com/%s",
                region, userPoolId
        );

        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(clientId)
                .acceptLeeway(5)
                .build();

        return verifier.verify(token);
    }
}