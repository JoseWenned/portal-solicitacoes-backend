package br.com.wenned.portalsolicitacoes.presentation.controller.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.solicitacoes.AlterarStatusSolicitacaoRequestDTO;
import br.com.wenned.portalsolicitacoes.application.dto.solicitacoes.EditarSolicitacaoRequestDTO;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.AlterarStatusSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.EditarSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.ExcluirSolicitacaoUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/solicitacoes")
public class OperacoesSolicitacaoController {

    private final EditarSolicitacaoUseCase editarUseCase;
    private final ExcluirSolicitacaoUseCase excluirUseCase;
    private final AlterarStatusSolicitacaoUseCase alterarStatusUseCase;

    public OperacoesSolicitacaoController(
        EditarSolicitacaoUseCase editarUseCase,
        ExcluirSolicitacaoUseCase excluirUseCase,
        AlterarStatusSolicitacaoUseCase alterarStatusUseCase
    ) {
        this.editarUseCase = editarUseCase;
        this.excluirUseCase = excluirUseCase;
        this.alterarStatusUseCase = alterarStatusUseCase;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody EditarSolicitacaoRequestDTO request
    ) {
        editarUseCase.executar(
            id,
            UUID.fromString(jwt.getSubject()),
            request.titulo(),
            request.descricao(),
            request.categoria()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        excluirUseCase.executar(
            id,
            UUID.fromString(jwt.getSubject())
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alterarStatus(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody AlterarStatusSolicitacaoRequestDTO request
    ) {
        alterarStatusUseCase.executar(
            id,
            UUID.fromString(jwt.getSubject()),
            request.status()
        );

        return ResponseEntity.noContent().build();
    }
}