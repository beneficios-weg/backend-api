package com.weg.benefits.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.weg.benefits.catalog.CatalogDtos.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=${CATALOG_TEST_DB_URL}"})
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "CATALOG_TEST_DB_URL", matches = ".*/benefits_catalog_test(?:\\?.*)?")
class CatalogIntegrationTests {
    @Autowired CatalogService catalog;
    @Autowired EstablishmentRepository establishments;
    @Autowired BenefitRepository benefits;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired Validator validator;

    @Test
    void listIncludesOnlineWhileMapContainsOnlyPhysicalAndHybridPartners() throws Exception {
        UUID categoryId = category();
        var benefit = benefit("Crachá", "20%");
        var online = catalog.createEstablishment(partner(categoryId, benefit.id(), EstablishmentChannel.ONLINE));
        catalog.createEstablishment(partner(categoryId, benefit.id(), EstablishmentChannel.PHYSICAL));
        catalog.createEstablishment(partner(categoryId, benefit.id(), EstablishmentChannel.HYBRID));
        establishments.flush();

        http.perform(get("/v1/establishments").param("categoryId", categoryId.toString())
                .with(user("colaborador").roles("COLLABORATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        http.perform(get("/v1/establishments").param("categoryId", categoryId.toString())
                .param("forMap", "true").with(user("colaborador").roles("COLLABORATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].mappable").value(true));
        http.perform(get("/v1/establishments").param("categoryId", categoryId.toString())
                .param("channel", "ONLINE").with(user("colaborador").roles("COLLABORATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(online.id().toString()))
                .andExpect(jsonPath("$.content[0].mappable").value(false));
        http.perform(get("/v1/establishments").param("categoryId", categoryId.toString())
                .param("query", "PARCEIRO").with(user("colaborador").roles("COLLABORATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        http.perform(get("/v1/establishments").param("categoryId", categoryId.toString())
                .param("query", "inexistente").with(user("colaborador").roles("COLLABORATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        http.perform(get("/v1/establishments/" + online.id() + "/benefits")
                .with(user("colaborador").roles("COLLABORATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void textSurvivesPersistenceExactlyAndEmptyTextIsRejected() throws Exception {
        String original = "  Aapresentar crachá.\nTexto com erro mantido. ".repeat(20);
        var created = benefit(original, "20%");
        benefits.flush();
        assertEquals(original, jdbc.queryForObject("select participation_instructions from benefit where id=?",
                String.class, created.id()));
        assertEquals("20%", jdbc.queryForObject("select description from benefit_discount where benefit_id=?",
                String.class, created.id()));
        http.perform(patch("/v1/admin/benefits/" + created.id()).contentType("application/json")
                .content("{\"participationInstructions\":\"  \"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void permissionsAndUnknownChannelRemainConsistent() throws Exception {
        http.perform(get("/v1/establishments")).andExpect(status().isUnauthorized());
        http.perform(post("/v1/admin/establishments").contentType("application/json").content("{}")
                .with(user("colaborador").roles("COLLABORATOR"))).andExpect(status().isForbidden());
        http.perform(get("/v1/establishments").param("channel", "INVALID")
                .with(user("colaborador").roles("COLLABORATOR"))).andExpect(status().isBadRequest());
    }

    @Test
    void physicalDefaultAndLocationValidationApplyToHttpWrites() throws Exception {
        UUID categoryId = category();
        var benefit = benefit("Crachá", "20%");
        var missing = partner(categoryId, benefit.id(), EstablishmentChannel.ONLINE);
        String onlineJson = json.writeValueAsString(missing);
        http.perform(post("/v1/admin/establishments").contentType("application/json").content(onlineJson)
                .with(user("admin").roles("ADMIN"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("ONLINE")).andExpect(jsonPath("$.mappable").value(false));
        String defaultPhysical = onlineJson.replace("\"channel\":\"ONLINE\"", "\"channel\":null");
        http.perform(post("/v1/admin/establishments").contentType("application/json").content(defaultPhysical)
                .with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        http.perform(post("/v1/admin/establishments").contentType("application/json")
                .content(json.writeValueAsString(partner(categoryId, benefit.id(), null)))
                .with(user("admin").roles("ADMIN"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("PHYSICAL"));
    }

    @Test
    void patchTransitionClearsPhysicalLocationAndRequiresItWhenSwitchingBack() throws Exception {
        var created = catalog.createEstablishment(partner(category(), benefit("Crachá", "20%").id(),
                EstablishmentChannel.PHYSICAL));
        http.perform(patch("/v1/admin/establishments/" + created.id()).contentType("application/json")
                .content("{\"channel\":\"ONLINE\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mappable").value(false));
        establishments.flush();
        assertNull(jdbc.queryForObject("select latitude from establishment where id=?", BigDecimal.class, created.id()));
        http.perform(patch("/v1/admin/establishments/" + created.id()).contentType("application/json")
                .content("{\"channel\":\"PHYSICAL\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void invalidSecondItemRollsBackBatchAndBenefitActivation() {
        UUID categoryId = category();
        var benefit = benefit("Crachá", "20%");
        long before = establishments.count();
        var valid = partner(categoryId, benefit.id(), EstablishmentChannel.ONLINE);
        var invalid = new EstablishmentCreateRequest("Inválido", "Descrição", null, null, null, null,
                null, null, null, null, null, null, categoryId, benefit.id(), List.of(), EstablishmentChannel.PHYSICAL);
        assertThrows(CatalogException.class, () -> catalog.createEstablishments(
                new EstablishmentBatchCreateRequest(List.of(valid, invalid))));
        assertEquals(before, establishments.count());
        assertFalse(catalog.getBenefit(benefit.id()).active());
        assertEquals(0, catalog.getBenefit(benefit.id()).establishmentCount());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "CATALOG_SOURCE_FIXTURE", matches = ".+")
    void all112LocalSourceDraftsValidateAndPersistThroughHttpBatchesWithoutRewriting() throws Exception {
        JsonNode root = json.readTree(Files.readString(Path.of(System.getenv("CATALOG_SOURCE_FIXTURE"))));
        JsonNode drafts = root.get("benefits");
        assertEquals(112, drafts.size());
        for (int start = 0; start < drafts.size(); start += 100) {
            var requests = new java.util.ArrayList<BenefitCreateRequest>();
            for (int position = start; position < Math.min(start + 100, drafts.size()); position++) {
                JsonNode item = drafts.get(position);
                BenefitCreateRequest request = json.treeToValue(item.get("payload"), BenefitCreateRequest.class);
                assertTrue(validator.validate(request).isEmpty(), item.get("reference").asText());
                requests.add(request);
            }
            var result = http.perform(post("/v1/admin/benefits/batch").contentType("application/json")
                    .content(json.writeValueAsString(new BenefitBatchCreateRequest(requests)))
                    .with(user("admin").roles("ADMIN"))).andExpect(status().isCreated()).andReturn();
            JsonNode responses = json.readTree(result.getResponse().getContentAsString());
            assertEquals(requests.size(), responses.size());
            benefits.flush();
            for (int index = 0; index < requests.size(); index++) {
                var request = requests.get(index);
                UUID id = UUID.fromString(responses.get(index).get("id").asText());
                assertEquals(request.participationInstructions(), jdbc.queryForObject(
                        "select participation_instructions from benefit where id=?", String.class, id));
                assertEquals(request.discounts().getFirst().description(), jdbc.queryForObject(
                        "select description from benefit_discount where benefit_id=?", String.class, id));
            }
        }
    }

    @Test
    void newOnlineLinkDoesNotReactivateManuallyDeactivatedSharedBenefit() {
        UUID categoryId = category();
        var benefit = benefit("Crachá", "20%");
        catalog.createEstablishment(partner(categoryId, benefit.id(), EstablishmentChannel.PHYSICAL));
        catalog.deactivateBenefit(benefit.id());
        catalog.createEstablishment(partner(categoryId, benefit.id(), EstablishmentChannel.ONLINE));
        assertFalse(catalog.getBenefit(benefit.id()).active());
        assertEquals(2, catalog.getBenefit(benefit.id()).establishmentCount());
    }

    private UUID category() {
        return catalog.createCategory(new CategoryCreateRequest("Categoria " + UUID.randomUUID(), "mdi:store", "#0D47A1")).id();
    }

    private BenefitResponse benefit(String instructions, String discount) {
        return catalog.createBenefit(new BenefitCreateRequest("Colaboradores", instructions, "Contato",
                List.of(new DiscountInput("20%", discount))));
    }

    private EstablishmentCreateRequest partner(UUID category, UUID benefit, EstablishmentChannel channel) {
        boolean online = channel == EstablishmentChannel.ONLINE;
        return new EstablishmentCreateRequest("Parceiro " + UUID.randomUUID(), "20%",
                online ? null : "89200000", online ? null : 100, online ? null : "Rua Teste",
                online ? null : "Centro", online ? null : "Joinville", online ? null : "SC",
                online ? null : new BigDecimal("-26.3"), online ? null : new BigDecimal("-48.8"),
                null, null, category, benefit, List.of(), channel);
    }
}
