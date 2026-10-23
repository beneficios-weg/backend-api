package com.weg.benefits.identity;

import java.util.Locale;

import com.weg.benefits.identity.IdentityDtos.CreateUserRequest;
import com.weg.benefits.identity.IdentityDtos.UpdateUserRequest;
import com.weg.benefits.identity.IdentityDtos.UserResponse;
import com.weg.benefits.identity.IdentityDtos.UserPageResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

@Service
public class IdentityService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CredentialMailSender credentialMailSender;
    private final ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions;

    public IdentityService(AppUserRepository users, PasswordEncoder passwordEncoder,
            CredentialMailSender credentialMailSender,
            ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.credentialMailSender = credentialMailSender;
        this.sessions = sessions;
    }

    @Transactional
    public UserResponse createByAdmin(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        Role role = request.role() == null ? Role.COLLABORATOR : request.role();
        assertNewIdentity(username, email);

        // The record is flushed before sending so SMTP failure rolls the transaction back.
        AppUser user = users.saveAndFlush(new AppUser(username, email,
                passwordEncoder.encode(request.password()), role));
        credentialMailSender.sendCredentials(email, username, request.password());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserPageResponse listUsers(int page, int requestedSize) {
        int size = Math.min(Math.max(requestedSize, 1), 100);
        Page<AppUser> result = users.findAllByOrderByIdAsc(
                PageRequest.of(Math.max(page, 0), size, Sort.by("id").ascending()));
        return new UserPageResponse(result.getContent().stream().map(UserResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Integer id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public AppUser lockActiveUserForEngagement(Integer id) {
        AppUser user = users.findByIdForUpdate(id)
                .orElseThrow(() -> new IdentityException(HttpStatus.UNAUTHORIZED, "Sessão inválida",
                        "Não foi possível validar a conta autenticada."));
        if (!user.isActive()) {
            throw new IdentityException(HttpStatus.UNAUTHORIZED, "Conta indisponível",
                    "A conta não está disponível para sincronizar operações.");
        }
        return user;
    }

    @Transactional
    public UserResponse updateUser(Integer id, UpdateUserRequest request) {
        if (request.password() != null) {
            throw new IdentityException(HttpStatus.UNPROCESSABLE_ENTITY, "Alteração não permitida",
                    "A senha não pode ser redefinida nesta API.");
        }
        if (!request.hasChanges()) {
            throw new IdentityException(HttpStatus.BAD_REQUEST, "Alteração vazia",
                    "Informe ao menos um campo para atualizar.");
        }
        AppUser user = findUser(id);
        String username = request.username() == null ? user.getUsername() : normalizeUsername(request.username());
        String email = request.email() == null ? user.getEmail() : normalizeEmail(request.email());
        if (users.existsByUsernameAndIdNot(username, id) || users.existsByEmailAndIdNot(email, id)) {
            throw new IdentityException(HttpStatus.CONFLICT, "Conta em uso",
                    "O nome de usuário ou e-mail já está associado a outra conta.");
        }
        String previousUsername = user.getUsername();
        Role requestedRole = request.role();
        if (user.getRole() == Role.ADMIN && requestedRole == Role.COLLABORATOR) {
            throw new IdentityException(HttpStatus.UNPROCESSABLE_ENTITY, "Alteração de papel não permitida",
                    "Uma conta ADMIN não pode ser rebaixada.");
        }
        user.updateIdentity(username, email, requestedRole);
        revokeSessions(previousUsername);
        return UserResponse.from(user);
    }

    @Transactional
    public void deactivateUser(Integer targetId, Integer administratorId) {
        if (targetId.equals(administratorId)) {
            throw new IdentityException(HttpStatus.FORBIDDEN, "Operação não permitida",
                    "Um administrador não pode desativar a própria conta.");
        }
        AppUser target = findUser(targetId);
        if (target.getRole() == Role.ADMIN) {
            throw new IdentityException(HttpStatus.FORBIDDEN, "Operação não permitida",
                    "Contas ADMIN não podem ser desativadas.");
        }
        if (!target.isActive()) {
            return;
        }
        target.deactivate();
        revokeSessions(target.getUsername());
    }

    @Transactional
    public void provisionFirstAdmin(String usernameInput, String emailInput, String rawPassword) {
        if (users.countByRole(Role.ADMIN) > 0) {
            throw new IdentityException(HttpStatus.CONFLICT, "ADMIN já provisionado",
                    "Já existe uma conta ADMIN. O provisionamento inicial foi cancelado.");
        }
        String username = normalizeUsername(usernameInput);
        String email = normalizeEmail(emailInput);
        validateUsername(username);
        validateEmail(email);
        validatePassword(rawPassword);
        assertNewIdentity(username, email);
        AppUser admin = users.saveAndFlush(new AppUser(username, email,
                passwordEncoder.encode(rawPassword), Role.ADMIN));
        credentialMailSender.sendCredentials(email, username, rawPassword);
    }

    @Transactional(readOnly = true)
    public UserResponse getByLogin(String login) {
        String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(normalizedLogin, normalizedLogin)
                .filter(AppUser::isActive)
                .map(UserResponse::from)
                .orElseThrow(() -> new IdentityException(HttpStatus.NOT_FOUND, "Conta não encontrada",
                        "A conta solicitada não existe."));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Integer id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional(readOnly = true)
    AppUser findUser(Integer id) {
        return users.findById(id).orElseThrow(() -> new IdentityException(HttpStatus.NOT_FOUND,
                "Conta não encontrada", "A conta solicitada não existe."));
    }

    static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void assertNewIdentity(String username, String email) {
        if (users.existsByUsername(username) || users.existsByEmail(email)) {
            throw new IdentityException(HttpStatus.CONFLICT, "Conta em uso",
                    "O nome de usuário ou e-mail já está associado a uma conta.");
        }
    }

    private void revokeSessions(String username) {
        FindByIndexNameSessionRepository<? extends Session> repository = sessions.getObject();
        repository.findByPrincipalName(username).values()
                .forEach(session -> repository.deleteById(session.getId()));
    }

    private void validateUsername(String username) {
        if (username == null || !username.matches("[a-z0-9._-]{3,75}")) {
            throw new IdentityException(HttpStatus.BAD_REQUEST, "Nome de usuário inválido",
                    "Use de 3 a 75 letras ASCII minúsculas, números, ponto, hífen ou sublinhado.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.length() > 254 || !email.matches("^[^@\\s]+@(?:gmail\\.com|estudante\\.sesisenai\\.org\\.br)$")) {
            throw new IdentityException(HttpStatus.BAD_REQUEST, "E-mail inválido",
                    "Use um endereço válido dos domínios gmail.com ou estudante.sesisenai.org.br.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new IdentityException(HttpStatus.BAD_REQUEST, "Senha inválida",
                    "A senha deve ter entre 6 e 72 caracteres.");
        }
    }
}
