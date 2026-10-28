package com.weg.benefits.identity;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AdminPasswordMaintenanceValidationTests {
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
