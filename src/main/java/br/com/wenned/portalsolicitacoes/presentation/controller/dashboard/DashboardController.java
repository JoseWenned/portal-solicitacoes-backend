package br.com.wenned.portalsolicitacoes.presentation.controller.dashboard;

import br.com.wenned.portalsolicitacoes.application.usecase.dashboard.ConsultarDashboardUseCase;
import br.com.wenned.portalsolicitacoes.presentation.dto.dashboard.DashboardResponseDTO;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final ConsultarDashboardUseCase useCase;

    public DashboardController(ConsultarDashboardUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> consultar(
        @AuthenticationPrincipal Jwt jwt
    ) {
        var resultado = useCase.executar(
            UUID.fromString(jwt.getSubject())
        );

        var response = new DashboardResponseDTO(
            resultado.total(),
            resultado.abertas(),
            resultado.emAtendimento(),
            resultado.concluidas()
        );

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(response);
    }
}