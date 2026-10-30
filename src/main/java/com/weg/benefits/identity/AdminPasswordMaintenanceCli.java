package com.weg.benefits.identity;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import com.weg.benefits.config.PasswordEncodingConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Standalone operator tool: no Spring context, HTTP endpoint, migrations or credential e-mail. */
public final class AdminPasswordMaintenanceCli {
    private AdminPasswordMaintenanceCli() { }

    public static void main(String[] args) {
        if (args.length != 1 || !(args[0].equals("--local") || args[0].equals("--render"))) {
            System.err.println("Use somente --local ou --render; nunca informe senhas em argumentos.");
            System.exit(2);
            return;
        }
        Console console = System.console();
        if (console == null) {
            System.err.println("A troca exige terminal interativo com senha sem eco. Nenhum dado foi alterado.");
            System.exit(2);
            return;
        }
        int exit = 0;
        try {
            run(args[0].equals("--render"), console);
        } catch (InvalidInput exception) {
            System.err.println(exception.getMessage()); // Only fixed, application-owned messages.
            exit = 2;
        } catch (SQLException | RuntimeException exception) {
            // Connection/driver errors can include SQL, host names or credentials. Do not print them.
            System.err.println(safeFailureReason(exception));
            System.err.println("Não foi possível confirmar a troca. Verifique a conexão e teste o login antes de repetir.");
            exit = 1;
        }
        System.exit(exit);
    }

    private static void run(boolean remote, Console console) throws SQLException {
        console.printf("Manutenção de senha ADMIN — ambiente %s%n", remote ? "RENDER / NEON" : "LOCAL");
        String host = remote ? required(console.readLine("Host DIRETO do Neon (sem protocolo e sem -pooler): ")) : "localhost";
        String port = remote ? "5432" : defaultValue(console.readLine("Porta PostgreSQL local [5432]: "), "5432");
        String database = defaultValue(console.readLine("Banco [%s]: ", remote ? "neondb" : "benefits"), remote ? "neondb" : "benefits");
        String dbUser = remote ? required(console.readLine("Usuário do PostgreSQL: "))
                : defaultValue(console.readLine("Usuário do PostgreSQL [benefits_dev]: "), "benefits_dev");
        String url = databaseUrl(remote, host, port, database);
        console.printf("Destino: %s:%s/%s. A operação altera somente a conta ADMIN informada.%n", host, port, database);

        char[] databasePassword = null;
        char[] currentPassword = null;
        char[] newPassword = null;
        char[] confirmation = null;
        Properties properties = new Properties();
        try {
            databasePassword = console.readPassword(remote ? "Senha do PostgreSQL: "
                    : "Senha do PostgreSQL (Enter usa o padrão exclusivo de desenvolvimento): ");
            if (databasePassword == null) throw new InvalidInput("Operação cancelada; nenhuma alteração enviada.");
            if (!remote && databasePassword.length == 0) databasePassword = "benefits_dev_only".toCharArray();
            String login = required(console.readLine("Username ou e-mail do ADMIN: "));
            currentPassword = console.readPassword("Senha ATUAL do ADMIN: ");
            newPassword = console.readPassword("NOVA senha (6 a 72 caracteres): ");
            confirmation = console.readPassword("Repita a NOVA senha: ");
            if (currentPassword == null || newPassword == null || confirmation == null)
                throw new InvalidInput("Operação cancelada; nenhuma alteração enviada.");
            if (!Arrays.equals(newPassword, confirmation))
                throw new InvalidInput("As novas senhas não coincidem; nenhuma alteração enviada.");
            validateNewPassword(newPassword);

            properties.setProperty("user", dbUser);
            properties.setProperty("password", new String(databasePassword));
            properties.setProperty("ApplicationName", "weg-benefits-admin-password-maintenance");
            try (Connection connection = DriverManager.getConnection(url, properties)) {
                console.printf("Conexão com o PostgreSQL autenticada. Validando a conta ADMIN.%n");
                int revoked = changePassword(connection, PasswordEncodingConfiguration.newPasswordEncoder(),
                        login, currentPassword, newPassword);
                console.printf("Senha alterada. Sessões anteriores removidas: %d. Entre novamente no ambiente escolhido.%n", revoked);
            }
        } finally {
            properties.clear();
            clear(databasePassword); clear(currentPassword); clear(newPassword); clear(confirmation);
        }
    }

    /** Fixed operator diagnostics only; never interpolate exception text, SQL or supplied values. */
    static String safeFailureReason(Throwable failure) {
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof javax.net.ssl.SSLException || cause instanceof java.security.cert.CertificateException)
                return "[TLS] Falha na conexão segura. Confira o JDK e seus certificados; não desative a validação TLS.";
            if (cause instanceof java.net.UnknownHostException)
                return "[DNS] Host não localizado. Confira o host direto do Neon e a conexão de rede.";
            if (cause instanceof java.net.SocketTimeoutException)
                return "[TIMEOUT] O banco não respondeu a tempo. Confira a rede e o estado do banco.";
            if (cause instanceof java.net.ConnectException)
                return "[REDE] Não foi possível alcançar o PostgreSQL. Confira host, porta, firewall e se o banco está iniciado.";
        }
        if (failure instanceof SQLException sql) {
            String state = sql.getSQLState();
            if (state != null && state.startsWith("28"))
                return "[DB_AUTH] O PostgreSQL recusou a autenticação. Confira o usuário e a senha DO BANCO no Neon/Compose; não são o login e a senha do ADMIN.";
            if ("3D000".equals(state))
                return "[DB_NAME] Banco não encontrado. Confira o nome do banco usado pela API.";
            if ("42P01".equals(state) || "42703".equals(state) || "3F000".equals(state))
                return "[DB_SCHEMA] Estrutura esperada não encontrada. Confira projeto, branch, banco e migrações da API; não recrie nem apague dados.";
            if ("42501".equals(state))
                return "[DB_PERMISSION] O usuário do PostgreSQL não tem as permissões necessárias para a manutenção.";
            if ("55P03".equals(state) || "40P01".equals(state))
                return "[DB_LOCK] A manutenção encontrou uma operação concorrente. Evite uso simultâneo da conta.";
            if (state != null && state.startsWith("08"))
                return "[DB_CONNECTION] Falha ao estabelecer ou manter a conexão PostgreSQL. Confira rede, banco e JAR atualizado.";
        }
        return "[CLI_FAILURE] Falha interna na ferramenta ou na operação. Use o JDK 21 e recompile o JAR; não compartilhe credenciais.";
    }

    static String databaseUrl(boolean remote, String host, String port, String database) {
        if (!database.matches("[A-Za-z0-9_-]{1,63}") || !port.matches("[0-9]{1,5}")
                || Integer.parseInt(port) < 1 || Integer.parseInt(port) > 65535)
            throw new InvalidInput("Banco ou porta inválidos; nenhuma conexão iniciada.");
        if (remote && (!host.matches("[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.neon\\.tech") || host.contains("-pooler")))
            throw new InvalidInput("Use o host direto do projeto Neon, sem protocolo, parâmetros ou -pooler.");
        if (!remote && !host.equals("localhost"))
            throw new InvalidInput("O ambiente local aceita somente localhost.");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database
                + "?connectTimeout=15&socketTimeout=30&sslmode=" + (remote ? "verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory" : "disable");
    }

    /** Owns the transaction on a dedicated connection; callers close it after this operation. */
    static int changePassword(Connection connection, PasswordEncoder encoder, String login,
            char[] currentPassword, char[] newPassword) throws SQLException {
        validateNewPassword(newPassword);
        if (currentPassword == null || currentPassword.length == 0)
            throw new InvalidInput("Informe a senha atual; nenhuma alteração enviada.");
        String normalized = required(login).toLowerCase(Locale.ROOT);
        connection.setAutoCommit(false);
        try {
            try (var timeout = connection.createStatement()) {
                timeout.execute("SET LOCAL lock_timeout = '5s'");
            }
            int id;
            String username;
            String previousHash;
            try (var select = connection.prepareStatement("""
                    SELECT id, username, password_hash, role, active FROM app_user
                    WHERE username = ? OR email = ? FOR UPDATE
                    """)) {
                select.setString(1, normalized); select.setString(2, normalized);
                try (var row = select.executeQuery()) {
                    if (!row.next() || !row.getBoolean("active") || !row.getString("role").equals("ADMIN"))
                        throw new InvalidInput("ADMIN ativo e senha atual não foram validados; nenhuma alteração realizada.");
                    id = row.getInt("id"); username = row.getString("username"); previousHash = row.getString("password_hash");
                    if (!encoder.matches(new String(currentPassword), previousHash))
                        throw new InvalidInput("ADMIN ativo e senha atual não foram validados; nenhuma alteração realizada.");
                    if (encoder.matches(new String(newPassword), previousHash))
                        throw new InvalidInput("A nova senha deve ser diferente da atual; nenhuma alteração realizada.");
                }
            }
            try (var update = connection.prepareStatement("UPDATE app_user SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                update.setString(1, encoder.encode(new String(newPassword))); update.setInt(2, id);
                if (update.executeUpdate() != 1) throw new SQLException("Unexpected update count");
            }
            int revoked;
            try (var delete = connection.prepareStatement("DELETE FROM spring_session WHERE principal_name = ?")) {
                delete.setString(1, username);
                revoked = delete.executeUpdate();
            }
            connection.commit();
            return revoked;
        } catch (SQLException | RuntimeException exception) {
            try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
            throw exception;
        }
    }

    static void validateNewPassword(char[] password) {
        if (password == null || password.length < 6 || password.length > 72 || new String(password).isBlank())
            throw new InvalidInput("A nova senha deve ter de 6 a 72 caracteres e não pode ser inteiramente em branco. Espaços não são removidos.");
    }

    private static String required(String input) {
        if (input == null || input.isBlank()) throw new InvalidInput("Entrada obrigatória ausente; operação cancelada.");
        return input.trim();
    }

    private static String defaultValue(String input, String fallback) {
        if (input == null) throw new InvalidInput("Operação cancelada; nenhuma alteração enviada.");
        return input.isBlank() ? fallback : input.trim();
    }

    private static void clear(char[] value) { if (value != null) Arrays.fill(value, '\0'); }

    static final class InvalidInput extends RuntimeException {
        InvalidInput(String message) { super(message); }
    }
}
