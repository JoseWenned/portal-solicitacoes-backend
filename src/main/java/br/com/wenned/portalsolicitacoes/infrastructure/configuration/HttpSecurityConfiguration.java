package br.com.wenned.portalsolicitacoes.infrastructure.configuration;

import br.com.wenned.portalsolicitacoes.application.port.out.autenticacao.SessaoAutenticacaoRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Clock;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class HttpSecurityConfiguration {

    @Bean
    public JwtDecoder jwtDecoder(
        SecretKey jwtSigningKey,
        SessaoAutenticacaoRepository sessoes,
        Clock clock,
        @Value("${portal.security.jwt.issuer}") String issuer,
        @Value("${portal.security.jwt.audience}") String audience
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(jwtSigningKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        OAuth2TokenValidator<Jwt> applicationValidator = jwt -> {
            if (jwt.getExpiresAt() == null
                || !jwt.getExpiresAt().isAfter(clock.instant())
                || !jwt.getAudience().contains(audience)) {
                return invalidToken();
            }

            UUID usuarioId;
            UUID sessaoId;

            try {
                usuarioId = UUID.fromString(jwt.getSubject());
                sessaoId = UUID.fromString(jwt.getClaimAsString("sid"));
            } catch (IllegalArgumentException | NullPointerException exception) {
                return invalidToken();
            }

            boolean active = sessoes.findById(sessaoId)
                .filter(sessao ->
                    sessao.getUsuarioId().equals(usuarioId)
                        && sessao.estaAtiva(clock.instant())
                )
                .isPresent();

            return active
                ? OAuth2TokenValidatorResult.success()
                : invalidToken();
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            applicationValidator
        ));

        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Value("${portal.security.cookie-secure}") boolean secure
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository =
            new CookieCsrfTokenRepository();

        csrfRepository.setHeaderName("X-CSRF-TOKEN");
        csrfRepository.setCookieCustomizer(cookie ->
            cookie.httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
        );

        RequestMatcher csrfRequired = request -> {
            if (!"POST".equals(request.getMethod())) {
                return false;
            }

            String path = request.getServletPath();

            return "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/refresh".equals(path)
                || "/api/v1/auth/logout".equals(path);
        };

        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .requireCsrfProtectionMatcher(csrfRequired)
            )
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ERROR)
                .permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/actuator/health",
                    "/actuator/health/**",
                    "/api/v1/auth/csrf",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/usuarios",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(
                        response,
                        401,
                        "UNAUTHORIZED",
                        "Autenticação necessária ou inválida."
                    )
                )
                .accessDeniedHandler((request, response, exception) ->
                    writeError(
                        response,
                        403,
                        "ACCESS_DENIED",
                        "Acesso negado ou token CSRF inválido."
                    )
                )
            )
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(jwt -> {})
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(
                        response,
                        401,
                        "UNAUTHORIZED",
                        "Autenticação necessária ou inválida."
                    )
                )
            );

        return http.build();
    }

    private static OAuth2TokenValidatorResult invalidToken() {
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error(
                "invalid_token",
                "Token ou sessão inválidos.",
                null
            )
        );
    }

    private static void writeError(
        HttpServletResponse response,
        int status,
        String code,
        String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        response.getWriter().write(
            "{\"status\":" + status
                + ",\"code\":\"" + code
                + "\",\"message\":\"" + message
                + "\",\"fieldErrors\":[]}"
        );
    }
}