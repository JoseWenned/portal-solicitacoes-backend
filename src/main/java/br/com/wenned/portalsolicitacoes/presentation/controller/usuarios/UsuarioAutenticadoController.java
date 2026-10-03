package br.com.wenned.portalsolicitacoes.presentation.controller.usuarios;

import br.com.wenned.portalsolicitacoes.application.dto.autenticacao.AutenticacaoResultadoDTO.UsuarioAutenticado;
import br.com.wenned.portalsolicitacoes.application.usecase.usuarios.ConsultarUsuarioAutenticadoUseCase;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioAutenticadoController {

    private final ConsultarUsuarioAutenticadoUseCase consultar;

    public UsuarioAutenticadoController(
        ConsultarUsuarioAutenticadoUseCase consultar
    ) {
        this.consultar = consultar;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioAutenticado> me(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store")
            .body(consultar.execute(
                UUID.fromString(jwt.getSubject())
            ));
    }
}