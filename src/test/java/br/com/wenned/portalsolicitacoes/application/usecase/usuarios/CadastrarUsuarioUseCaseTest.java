package br.com.wenned.portalsolicitacoes.application.usecase.usuarios;

import br.com.wenned.portalsolicitacoes.application.port.out.security.PasswordHasher;
import br.com.wenned.portalsolicitacoes.application.port.out.usuarios.UsuarioRepository;
import br.com.wenned.portalsolicitacoes.domain.entity.usuarios.Usuario;
import br.com.wenned.portalsolicitacoes.domain.exception.usuarios.EmailJaCadastradoException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadastrarUsuarioUseCaseTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    PasswordHasher passwordHasher;

    CadastrarUsuarioUseCase useCase;

    private static final Instant NOW =
        Instant.parse("2026-10-02T06:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = new CadastrarUsuarioUseCase(
            usuarioRepository,
            passwordHasher,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void deveCadastrarAnaNormalizandoNomeEmailEProtegendoSenha() {
        when(passwordHasher.hash("senha12345")).thenReturn("hash-de-teste");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = useCase.execute(
            " Ana ",
            " ANA@EXAMPLE.COM ",
            "senha12345"
        );

        ArgumentCaptor<Usuario> captor =
            ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).existsByEmail("ana@example.com");
        verify(usuarioRepository).save(captor.capture());

        Usuario persisted = captor.getValue();

        assertThat(persisted.getPasswordHash()).isEqualTo("hash-de-teste");
        assertThat(persisted.toString()).doesNotContain("hash-de-teste");

        assertThat(resultado.id()).isEqualTo(persisted.getId());
        assertThat(resultado.name()).isEqualTo("Ana");
        assertThat(resultado.email()).isEqualTo("ana@example.com");
        assertThat(resultado.createdAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", "An", " An "})
    void deveRecusarNomeComMenosDeTresCaracteres(String name) {
        assertThatThrownBy(() ->
            useCase.execute(name, "ana@example.com", "senha12345")
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(usuarioRepository, passwordHasher);
    }

    @Test
    void deveRecusarNomeAcimaDeCemCaracteres() {
        assertThatThrownBy(() ->
            useCase.execute(
                "A".repeat(101),
                "ana@example.com",
                "senha12345"
            )
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(usuarioRepository, passwordHasher);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ana", "ana@", "ana @example.com"})
    void deveRecusarEmailInvalido(String email) {
        assertThatThrownBy(() ->
            useCase.execute("Ana", email, "senha12345")
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(usuarioRepository, passwordHasher);
    }

    @Test
    void deveRecusarEmailJaCadastradoAntesDeGerarHash() {
        when(usuarioRepository.existsByEmail("ana@example.com"))
            .thenReturn(true);

        assertThatThrownBy(() ->
            useCase.execute("Ana", "ANA@example.com", "senha12345")
        ).isInstanceOf(EmailJaCadastradoException.class);

        verifyNoInteractions(passwordHasher);
        verify(usuarioRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "1234567"})
    void deveRecusarSenhaInvalida(String password) {
        assertThatThrownBy(() ->
            useCase.execute("Ana", "ana@example.com", password)
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(usuarioRepository, passwordHasher);
    }

    @Test
    void deveRecusarSenhaAcimaDoLimiteEmBytes() {
        assertThatThrownBy(() ->
            useCase.execute(
                "Ana",
                "ana@example.com",
                "á".repeat(37)
            )
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(usuarioRepository, passwordHasher);
    }

    @Test
    void devePreservarIdentidadeAoReconstituirUsuario() {
        Usuario original = Usuario.criar(
            "Ana", "ana@example.com", "hash-original", NOW
        );

        Usuario reconstituido = Usuario.reconstituir(
            original.getId(),
            "Ana Maria",
            "ana@example.com",
            "outro-hash",
            NOW
        );

        assertThat(reconstituido).isEqualTo(original);
        assertThat(reconstituido.hashCode()).isEqualTo(original.hashCode());

        Usuario outroUsuario = Usuario.criar(
            "Ana", "ana@example.com", "hash-original", NOW
        );

        assertThat(outroUsuario).isNotEqualTo(original);
    }
}