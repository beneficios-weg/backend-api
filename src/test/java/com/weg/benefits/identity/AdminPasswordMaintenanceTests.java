package com.weg.benefits.identity;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.weg.benefits.config.PasswordEncodingConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnabledIfEnvironmentVariable(named = "IDENTITY_MAINTENANCE_TEST_DB_URL",
        matches = "jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/benefits_identity_maintenance_test")
class AdminPasswordMaintenanceTests {
    private final PasswordEncoder encoder = PasswordEncodingConfiguration.newPasswordEncoder();
    private final String url = System.getenv("IDENTITY_MAINTENANCE_TEST_DB_URL");
    private final String dbUser = System.getenv().getOrDefault("IDENTITY_MAINTENANCE_TEST_DB_USER", "benefits_dev");
    private final String dbPassword = System.getenv().getOrDefault("IDENTITY_MAINTENANCE_TEST_DB_PASSWORD", "benefits_dev_only");
    private String schema;
    private String oldHash;

    @BeforeEach
    void prepareIsolatedSchema() throws Exception {
        schema = "maintenance_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(url, dbUser, dbPassword).schemas(schema).defaultSchema(schema)
                .target("1").load().migrate();
        oldHash = encoder.encode("old-fixture-password");
        try (var connection = connection(); var insert = connection.prepareStatement("""
                INSERT INTO app_user (username,email,password_hash,role,created_at,updated_at)
                VALUES ('admin','admin@gmail.com',?,'ADMIN','2020-01-01T00:00:00Z','2020-01-01T00:00:00Z'),
                ('other_admin','other@gmail.com',?,'ADMIN','2020-01-01T00:00:00Z','2020-01-01T00:00:00Z')
                """)) {
            insert.setString(1, oldHash); insert.setString(2, oldHash); insert.executeUpdate();
            session(connection, "admin"); session(connection, "admin"); session(connection, "other_admin");
        }
    }

    @AfterEach
    void removeOnlyCreatedSchema() throws Exception {
        if (schema == null || !schema.matches("maintenance_[a-f0-9]{32}")) return;
        try (var connection = DriverManager.getConnection(url, dbUser, dbPassword); var sql = connection.createStatement()) {
            sql.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private Connection connection() throws SQLException {
        var connection = DriverManager.getConnection(url, dbUser, dbPassword);
        try (var sql = connection.createStatement()) { sql.execute("SET search_path TO " + schema); }
        return connection;
    }

    private void session(Connection connection, String username) throws Exception {
        String id = UUID.randomUUID().toString();
        try (var sql = connection.prepareStatement("""
                INSERT INTO spring_session (primary_id,session_id,creation_time,last_access_time,max_inactive_interval,expiry_time,principal_name)
                VALUES (?,?,1,1,-1,9999999999999,?)
                """)) {
            sql.setString(1, id); sql.setString(2, UUID.randomUUID().toString()); sql.setString(3, username); sql.executeUpdate();
        }
        try (var sql = connection.prepareStatement("INSERT INTO spring_session_attributes VALUES (?,'fixture',?)")) {
            sql.setString(1, id); sql.setBytes(2, new byte[] {1, 2}); sql.executeUpdate();
        }
    }

    private String hash(String username) throws Exception {
        try (var connection = connection(); var sql = connection.prepareStatement("SELECT password_hash FROM app_user WHERE username=?")) {
            sql.setString(1, username);
            try (var row = sql.executeQuery()) { assertTrue(row.next()); return row.getString(1); }
        }
    }

    private long count(String table) throws Exception {
        try (var connection = connection(); var sql = connection.createStatement(); var row = sql.executeQuery("SELECT count(*) FROM " + table)) {
            assertTrue(row.next()); return row.getLong(1);
        }
    }

    private int rotate(String login, String current, String replacement) throws Exception {
        try (var connection = connection()) {
            return AdminPasswordMaintenanceCli.changePassword(connection, encoder, login, current.toCharArray(), replacement.toCharArray());
        }
    }

    @Test
    void rotatesOnlyTargetAdminWithApiCompatibleHashAndRevokesOnlyItsSessions() throws Exception {
        assertEquals(2, rotate("  ADMIN@gmail.com  ", "old-fixture-password", "  nova Senha💙  "));
        String stored = hash("admin");
        assertNotEquals(oldHash, stored);
        assertTrue(PasswordEncodingConfiguration.newPasswordEncoder().matches("  nova Senha💙  ", stored));
        assertFalse(encoder.matches("old-fixture-password", stored));
        assertFalse(encoder.matches("nova Senha💙", stored), "Password must not be trimmed");
        assertEquals(oldHash, hash("other_admin"));
        assertEquals(2, count("app_user"));
        assertEquals(1, count("spring_session"));
        assertEquals(1, count("spring_session_attributes"));
        try (var connection = connection(); var sql = connection.createStatement(); var row = sql.executeQuery("""
                SELECT id,username,email,role,active,created_at,updated_at FROM app_user WHERE username='admin'
                """)) {
            assertTrue(row.next()); assertEquals(1, row.getInt("id")); assertEquals("admin", row.getString("username"));
            assertEquals("admin@gmail.com", row.getString("email")); assertEquals("ADMIN", row.getString("role"));
            assertTrue(row.getBoolean("active"));
            assertEquals(java.time.Instant.parse("2020-01-01T00:00:00Z"), row.getTimestamp("created_at").toInstant());
            assertTrue(row.getTimestamp("updated_at").after(row.getTimestamp("created_at")));
        }
    }

    @Test
    void wrongCurrentPasswordDoesNotChangeHashOrSessions() throws Exception {
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> rotate("admin", "wrong-fixture", "new-fixture-password"));
        assertEquals(oldHash, hash("admin")); assertEquals(3, count("spring_session"));
    }

    @Test
    void missingInactiveAndCollaboratorAccountsCannotBeRotated() throws Exception {
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> rotate("missing", "old-fixture-password", "new-fixture-password"));
        try (var connection = connection(); var sql = connection.createStatement()) { sql.executeUpdate("UPDATE app_user SET active=false WHERE username='admin'"); }
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> rotate("admin", "old-fixture-password", "new-fixture-password"));
        try (var connection = connection(); var sql = connection.createStatement()) { sql.executeUpdate("UPDATE app_user SET active=true,role='COLLABORATOR' WHERE username='admin'"); }
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> rotate("admin", "old-fixture-password", "new-fixture-password"));
        assertEquals(oldHash, hash("admin")); assertEquals(3, count("spring_session"));
    }

    @Test
    void invalidAndUnchangedNewPasswordsLeaveAccountUntouched() throws Exception {
        for (String replacement : new String[] {"12345", "x".repeat(73), "old-fixture-password"}) {
            assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> rotate("admin", "old-fixture-password", replacement));
        }
        assertEquals(oldHash, hash("admin")); assertEquals(3, count("spring_session"));
    }

    @Test
    void passwordUpdateRollsBackWhenSessionRevocationFails() throws Exception {
        try (var connection = connection(); var sql = connection.createStatement()) {
            sql.execute("CREATE FUNCTION reject_session_delete() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fixture error'; END $$");
            sql.execute("CREATE TRIGGER fixture_failure BEFORE DELETE ON spring_session FOR EACH ROW EXECUTE FUNCTION reject_session_delete()");
        }
        assertThrows(SQLException.class, () -> rotate("admin", "old-fixture-password", "new-fixture-password"));
        assertEquals(oldHash, hash("admin")); assertEquals(3, count("spring_session")); assertEquals(3, count("spring_session_attributes"));
    }

    @Test
    void concurrentRotationsCannotBothUseThePreviousPassword() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attempt(start, "first-fixture-password"));
            var second = executor.submit(() -> attempt(start, "second-fixture-password"));
            start.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, count("spring_session"));
    }

    private boolean attempt(CountDownLatch start, String replacement) throws Exception {
        start.await();
        try { rotate("admin", "old-fixture-password", replacement); return true; }
        catch (AdminPasswordMaintenanceCli.InvalidInput exception) { return false; }
    }
}
