package br.com.wenned.portalsolicitacoes.presentation.controller.usuarios;

import br.com.wenned.portalsolicitacoes.application.usecase.usuarios.CadastrarUsuarioUseCase;
import br.com.wenned.portalsolicitacoes.presentation.dto.usuarios.CadastrarUsuarioRequestDTO;
import br.com.wenned.portalsolicitacoes.presentation.dto.usuarios.UsuarioResponseDTO;

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
    public ResponseEntity<UsuarioResponseDTO> cadastrar(
        @Valid @RequestBody CadastrarUsuarioRequestDTO request
    ) {
        var resultado = cadastrarUsuario.execute(
            request.name(),
            request.email(),
            request.password()
        );

        UsuarioResponseDTO response = new UsuarioResponseDTO(
            resultado.id(),
            resultado.name(),
            resultado.email(),
            resultado.createdAt()
        );

        return ResponseEntity.status(201).body(response);
    }
}