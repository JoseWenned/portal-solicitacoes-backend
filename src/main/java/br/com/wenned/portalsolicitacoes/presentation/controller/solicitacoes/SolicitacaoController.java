package br.com.wenned.portalsolicitacoes.presentation.controller.solicitacoes;

import br.com.wenned.portalsolicitacoes.application.dto.result.PaginaResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.dto.solicitacoes.ConsultarSolicitacaoResultadoDTO;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.ConsultarSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.CriarSolicitacaoUseCase;
import br.com.wenned.portalsolicitacoes.application.usecase.solicitacoes.ListarSolicitacoesUseCase;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.CategoriaSolicitacao;
import br.com.wenned.portalsolicitacoes.domain.entity.solicitacoes.StatusSolicitacao;
import br.com.wenned.portalsolicitacoes.presentation.dto.solicitacoes.CriarSolicitacaoRequestDTO;
import br.com.wenned.portalsolicitacoes.presentation.dto.solicitacoes.SolicitacaoResponseDTO;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoController {

    private final CriarSolicitacaoUseCase criarSolicitacaoUseCase;
    private final ConsultarSolicitacaoUseCase consultarSolicitacaoUseCase;
    private final ListarSolicitacoesUseCase listarSolicitacoesUseCase;

    public SolicitacaoController(
        CriarSolicitacaoUseCase criarSolicitacaoUseCase,
        ConsultarSolicitacaoUseCase consultarSolicitacaoUseCase,
        ListarSolicitacoesUseCase listarSolicitacoesUseCase
    ) {
        this.criarSolicitacaoUseCase = criarSolicitacaoUseCase;
        this.consultarSolicitacaoUseCase = consultarSolicitacaoUseCase;
        this.listarSolicitacoesUseCase = listarSolicitacoesUseCase;
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

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoResponseDTO> consultar(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        var resultado = consultarSolicitacaoUseCase.executar(
            id,
            UUID.fromString(jwt.getSubject())
        );

        return ResponseEntity.ok(toResponse(resultado));
    }

    @GetMapping
    public ResponseEntity<PaginaResultadoDTO<SolicitacaoResponseDTO>> listar(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "status", required = false)
        StatusSolicitacao status,
        @RequestParam(name = "categoria", required = false)
        CategoriaSolicitacao categoria,
        @RequestParam(name = "titulo", required = false)
        String titulo,
        @RequestParam(name = "dataInicial", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataInicial,
        @RequestParam(name = "dataFinal", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataFinal
    ) {
        var resultado = listarSolicitacoesUseCase.executar(
            UUID.fromString(jwt.getSubject()),
            status,
            categoria,
            titulo,
            dataInicial,
            dataFinal,
            page,
            size
        );

        var content = resultado.content().stream()
            .map(this::toResponse)
            .toList();

        var response = new PaginaResultadoDTO<>(
            content,
            resultado.page(),
            resultado.size(),
            resultado.totalElements(),
            resultado.totalPages()
        );

        return ResponseEntity.ok(response);
    }

    private SolicitacaoResponseDTO toResponse(
        ConsultarSolicitacaoResultadoDTO resultado
    ) {
        return new SolicitacaoResponseDTO(
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
    }
}