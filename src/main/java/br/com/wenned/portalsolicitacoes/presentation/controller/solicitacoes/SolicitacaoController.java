package br.com.wenned.portalsolicitacoes.presentation.controller.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.CriarSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.presentation.dto.solicitacoes.CriarSolicitacaoRequestDTO;
import br.com.wenned.portalsolicitacoes.presentation.dto.solicitacoes.SolicitacaoResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoController {

    private final CriarSolicitacaoUseCase criarSolicitacaoUseCase;

    public SolicitacaoController(
        CriarSolicitacaoUseCase criarSolicitacaoUseCase
    ) {
        this.criarSolicitacaoUseCase = criarSolicitacaoUseCase;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoResponseDTO> criar(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CriarSolicitacaoRequestDTO request
    ) {
        UUID solicitanteId = UUID.fromString(jwt.getSubject());

        var resultado = criarSolicitacaoUseCase.executar(
            solicitanteId,
            request.titulo(),
            request.descricao(),
            request.categoria()
        );

        var response = new SolicitacaoResponseDTO(
            resultado.id(),
            resultado.codigo(),
            resultado.titulo(),
            resultado.descricao(),
            resultado.categoria(),
            resultado.status(),
            resultado.solicitanteId(),
            resultado.createdAt(),
            resultado.updatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}