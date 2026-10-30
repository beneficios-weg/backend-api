package com.weg.benefits.identity;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AdminPasswordMaintenanceValidationTests {
    @Test
    void diagnosticsDistinguishDatabaseFailuresWithoutEchoingSensitiveExceptionText() {
        String sensitive = "fixture-password jdbc:postgresql://private-host SQL secret";
        for (String[] sample : new String[][] {
                {"28P01", "DB_AUTH"}, {"28000", "DB_AUTH"}, {"3D000", "DB_NAME"},
                {"42P01", "DB_SCHEMA"}, {"42703", "DB_SCHEMA"}, {"3F000", "DB_SCHEMA"},
                {"42501", "DB_PERMISSION"}, {"55P03", "DB_LOCK"}, {"40P01", "DB_LOCK"},
                {"08001", "DB_CONNECTION"}, {"08007", "DB_CONNECTION"}, {"XX000", "CLI_FAILURE"}}) {
            String diagnostic = AdminPasswordMaintenanceCli.safeFailureReason(new java.sql.SQLException(sensitive, sample[0]));
            assertTrue(diagnostic.startsWith("[" + sample[1] + "]"));
            assertFalse(diagnostic.contains(sensitive));
        }
        assertTrue(AdminPasswordMaintenanceCli.safeFailureReason(new IllegalStateException(sensitive)).startsWith("[CLI_FAILURE]"));
        assertTrue(AdminPasswordMaintenanceCli.safeFailureReason(new java.sql.SQLException(sensitive, (String) null)).startsWith("[CLI_FAILURE]"));
    }

    @Test
    void networkAndTlsCausesTakePrecedenceWithoutExposingTheirMessages() {
        for (Throwable[] sample : new Throwable[][] {
                {new javax.net.ssl.SSLHandshakeException("fixture secret"), new IllegalStateException("TLS")},
                {new java.net.UnknownHostException("fixture secret"), new IllegalStateException("DNS")},
                {new java.net.SocketTimeoutException("fixture secret"), new IllegalStateException("TIMEOUT")},
                {new java.net.ConnectException("fixture secret"), new IllegalStateException("REDE")}}) {
            var failure = new java.sql.SQLException("fixture SQL secret", "08001", sample[0]);
            String diagnostic = AdminPasswordMaintenanceCli.safeFailureReason(failure);
            assertTrue(diagnostic.startsWith("[" + sample[1].getMessage() + "]"));
            assertFalse(diagnostic.contains("fixture"));
        }
    }

    @Test
    void remoteRequiresDirectNeonHostAndVerifiedTlsWithoutCredentialParameters() {
        String url = AdminPasswordMaintenanceCli.databaseUrl(true, "ep-example.us-east-2.aws.neon.tech", "5432", "neondb");
        assertTrue(url.contains("sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory"));
        for (String host : new String[] {"localhost", "example.com", "ep-example-pooler.aws.neon.tech", "user:secret@ep-test.neon.tech", "ep-test.neon.tech?sslmode=disable"}) {
            assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.databaseUrl(true, host, "5432", "neondb"));
        }
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.databaseUrl(true, "ep-test.neon.tech", "5432", "neondb?password=secret"));
    }

    @Test
    void localCannotAccidentallyTargetRemoteAndPortIsValidated() {
        assertTrue(AdminPasswordMaintenanceCli.databaseUrl(false, "localhost", "5432", "benefits").startsWith("jdbc:postgresql://localhost:5432/benefits?"));
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.databaseUrl(false, "ep-test.neon.tech", "5432", "benefits"));
        for (String port : new String[] {"0", "65536", "-1", "5432/other"})
            assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.databaseUrl(false, "localhost", port, "benefits"));
    }

    @Test
    void passwordUsesExistingSixToSeventyTwoCharacterPolicyWithoutNormalization() {
        AdminPasswordMaintenanceCli.validateNewPassword("   a  ".toCharArray());
        AdminPasswordMaintenanceCli.validateNewPassword("x".repeat(72).toCharArray());
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.validateNewPassword(null));
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.validateNewPassword("      ".toCharArray()));
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.validateNewPassword("12345".toCharArray()));
        assertThrows(AdminPasswordMaintenanceCli.InvalidInput.class, () -> AdminPasswordMaintenanceCli.validateNewPassword("x".repeat(73).toCharArray()));
    }
}
