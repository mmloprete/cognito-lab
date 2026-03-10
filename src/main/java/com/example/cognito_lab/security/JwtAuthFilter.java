package com.example.cognito_lab.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final CognitoJwtValidator jwtValidator;

    public JwtAuthFilter(CognitoJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return "/ping".equals(path)
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            res.sendError(HttpStatus.UNAUTHORIZED.value(), "Token requerido");
            return;
        }

        try {
            String token = header.substring(7);
            DecodedJWT jwt = jwtValidator.validate(token);

            String email = jwt.getClaim("email").asString();
            String subject = jwt.getSubject();
            String principal = (email != null && !email.isBlank()) ? email : subject;

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_PARTNER"))
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);

        } catch (JWTVerificationException ex) {
            SecurityContextHolder.clearContext();
            res.sendError(HttpStatus.UNAUTHORIZED.value(), "Token inválido");
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            res.sendError(HttpStatus.UNAUTHORIZED.value(), "No se pudo validar el token");
        }
    }
}