package br.com.wenned.portalsolicitacoes.presentation.controller.autenticacao;

import br.com.wenned.portalsolicitacoes.application.dto.autenticacao.AutenticacaoResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.usecase.autenticacao.AutenticacaoUseCase;
import br.com.wenned.portalsolicitacoes.presentation.dto.autenticacao.LoginRequestDTO;
import br.com.wenned.portalsolicitacoes.presentation.dto.autenticacao.LoginResponseDTO;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {

    private static final String COOKIE_NAME = "refresh_token";

    private final AutenticacaoUseCase autenticacao;
    private final Clock clock;
    private final boolean secure;

    public AutenticacaoController(
        AutenticacaoUseCase autenticacao,
        Clock clock,
        @Value("${portal.security.cookie-secure}") boolean secure
    ) {
        this.autenticacao = autenticacao;
        this.clock = clock;
        this.secure = secure;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(new CsrfResponse(
                    token.getHeaderName(),
                    token.getToken()
            ));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
        @Valid @RequestBody LoginRequestDTO request
    ) {
        return tokenResponse(
            autenticacao.login(request.email(), request.password())
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(
        @CookieValue(name = COOKIE_NAME, required = false)
        String refreshToken
    ) {
        return tokenResponse(autenticacao.renovar(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = COOKIE_NAME, required = false)
        String refreshToken
    ) {
        autenticacao.logout(refreshToken);

        return ResponseEntity.noContent()
            .header(
                HttpHeaders.SET_COOKIE,
                cookie("", Duration.ZERO).toString()
            )
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .build();
    }

    private ResponseEntity<LoginResponseDTO> tokenResponse(
        AutenticacaoResultadoDTO resultado
    ) {
        Duration remaining = Duration.between(
            clock.instant(),
            resultado.sessionExpiresAt()
        );

        if (remaining.isNegative()) {
            remaining = Duration.ZERO;
        }

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                cookie(resultado.refreshToken(), remaining).toString()
            )
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(LoginResponseDTO.from(resultado));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(maxAge)
            .build();
    }

    public record CsrfResponse(String headerName, String token) {
    }
}