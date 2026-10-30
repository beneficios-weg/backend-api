package com.weg.benefits.catalog;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "CATALOG_TEST_DB_URL", matches = ".*/benefits_catalog_test(?:\\?.*)?")
class CatalogMigrationTests {
    @Test
    void upgradingV6PreservesTextCoordinatesAndSharedLinks() throws Exception {
        String url = System.getenv("CATALOG_TEST_DB_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(url, username, password);
                var sql = connection.createStatement()) {
            Flyway.configure().dataSource(url, username, password).schemas(schema).defaultSchema(schema)
                    .target("6").load().migrate();
            sql.execute("SET search_path TO " + schema);
            sql.execute("""
                    INSERT INTO catalog_category(id,name,icon_key,color_hex)
                    VALUES ('10000000-0000-0000-0000-000000000001','Categoria legada','mdi:store','#0D47A1');
                    INSERT INTO benefit(id,audience,participation_instructions,contact,active,first_linked_at)
                    VALUES ('20000000-0000-0000-0000-000000000001','Colaboradores',
                    'Apresente seu crachá WEG na loja antes de realizar o pagamento.','Contato legado',false,CURRENT_TIMESTAMP);
                    INSERT INTO benefit_discount(id,benefit_id,value,description)
                    VALUES ('30000000-0000-0000-0000-000000000001',
                    '20000000-0000-0000-0000-000000000001','20%','Descrição legada com erro manttido.');
                    INSERT INTO establishment(name,description,postal_code,street_number,street,district,city,state,
                    latitude,longitude,category_id,benefit_id)
                    SELECT 'Unidade ' || n, 'Descrição de estabelecimento legado que deve permanecer intacta.',
                    '89200000',100,'Rua Teste','Centro','Joinville','SC',-26.300000,-48.800000,
                    '10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001'
                    FROM generate_series(1,2) n;
                    """);
            Flyway.configure().dataSource(url, username, password).schemas(schema).defaultSchema(schema)
                    .load().migrate();
            try (var original = sql.executeQuery("SELECT name FROM catalog_category WHERE id='10000000-0000-0000-0000-000000000001'")) {
                assertTrue(original.next());
                assertEquals("Categoria legada", original.getString(1));
            }
            sql.executeUpdate("UPDATE catalog_category SET name='Lazer' WHERE id='10000000-0000-0000-0000-000000000001'");
            assertThrows(java.sql.SQLException.class, () -> sql.executeUpdate(
                    "UPDATE catalog_category SET name='Laze' WHERE id='10000000-0000-0000-0000-000000000001'"));
            try (var result = sql.executeQuery("""
                    SELECT count(*),count(DISTINCT e.benefit_id),bool_and(e.channel='PHYSICAL'),
                    bool_and(e.latitude=-26.300000 AND e.longitude=-48.800000),
                    min(b.participation_instructions),bool_and(NOT b.active),min(d.description)
                    FROM establishment e JOIN benefit b ON b.id=e.benefit_id
                    JOIN benefit_discount d ON d.benefit_id=b.id
                    """)) {
                assertTrue(result.next());
                assertEquals(2, result.getInt(1));
                assertEquals(1, result.getInt(2));
                assertTrue(result.getBoolean(3));
                assertTrue(result.getBoolean(4));
                assertEquals("Apresente seu crachá WEG na loja antes de realizar o pagamento.", result.getString(5));
                assertTrue(result.getBoolean(6));
                assertEquals("Descrição legada com erro manttido.", result.getString(7));
            }
        } finally {
            try (var connection = DriverManager.getConnection(url, username, password);
                    var sql = connection.createStatement()) {
                sql.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
