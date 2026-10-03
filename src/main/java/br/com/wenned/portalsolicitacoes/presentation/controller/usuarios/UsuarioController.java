package br.com.wenned.portalsolicitacoes.presentation.controller.usuarios;

import br.com.wenned.portalsolicitacoes.application.usecase.usuarios.CadastrarUsuarioUseCase;
import br.com.wenned.portalsolicitacoes.presentation.dto.usuarios.CadastrarUsuarioRequest;
import br.com.wenned.portalsolicitacoes.presentation.dto.usuarios.UsuarioResponse;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final CadastrarUsuarioUseCase cadastrarUsuario;

    public UsuarioController(CadastrarUsuarioUseCase cadastrarUsuario) {
        this.cadastrarUsuario = cadastrarUsuario;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(
        @Valid @RequestBody CadastrarUsuarioRequest request
    ) {
        var resultado = cadastrarUsuario.execute(
            request.name(),
            request.email(),
            request.password()
        );

        UsuarioResponse response = new UsuarioResponse(
            resultado.id(),
            resultado.name(),
            resultado.email(),
            resultado.createdAt()
        );

        return ResponseEntity.status(201).body(response);
    }
}