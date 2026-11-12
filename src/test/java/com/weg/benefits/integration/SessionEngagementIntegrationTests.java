package com.weg.benefits.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.weg.benefits.catalog.CatalogDtos.*;
import com.weg.benefits.catalog.CatalogService;
import com.weg.benefits.catalog.EstablishmentChannel;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Real security filters, JDBC sessions and committed transactions; only Google HTTP is simulated. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=8081",
        "server.address=127.0.0.1",
        "server.servlet.session.cookie.secure=false",
        "spring.datasource.url=${API_TEST_DB_URL}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.mail.google.client-id=test-client",
        "app.mail.google.client-secret=test-secret",
        "app.mail.google.refresh-token=test-refresh",
        "app.mail.google.sender=wegbenefits@gmail.com"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(SessionEngagementIntegrationTests.MailConfiguration.class)
@EnabledIfEnvironmentVariable(named = "API_TEST_DB_URL",
        matches = "jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/benefits_api_test")
class SessionEngagementIntegrationTests {
    private static final String PASSWORD = "Fixture-test-123";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private static final String EVENT_TIME = "2020-01-02T12:00:00Z";

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired CatalogService catalog;
    @Autowired GmailStub gmail;

    private Account admin;
    private Account alice;
    private Account bob;
    private UUID physical;
    private UUID online;

    @BeforeEach
    void fixtures() {
        gmail.server.reset();
        String hash = passwords.encode(PASSWORD);
        admin = account("admin", "ADMIN", hash);
        alice = account("alice", "COLLABORATOR", hash);
        bob = account("bob", "COLLABORATOR", hash);
        UUID category = catalog.createCategory(new CategoryCreateRequest(
                "Categoria " + UUID.randomUUID(), "mdi:store", "#0D47A1")).id();
        UUID benefit = catalog.createBenefit(new BenefitCreateRequest("Colaboradores", "Apresente o crachá.",
                "Contato de teste", List.of(new DiscountInput("20%", "Produtos selecionados.")))).id();
        physical = place(category, benefit, EstablishmentChannel.PHYSICAL);
        online = place(category, benefit, EstablishmentChannel.ONLINE);
    }

    @AfterEach
    void verifyGoogleHttpExpectations() {
        gmail.server.verify();
    }

    @Test
    void embeddedServerUsesPersistentHttpOnlyCookiesAndRejectsTheOldCookieAfterLogout() throws Exception {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpResponse<String> loggedIn = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8081/v1/auth/login"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                            Map.of("login", alice.username(), "password", PASSWORD)))).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(loggedIn.statusCode()).isEqualTo(200);
            String setCookie = loggedIn.headers().firstValue("Set-Cookie").orElseThrow();
            assertThat(setCookie).contains("HttpOnly", "SameSite=Lax", "Max-Age=2147483647");
            String cookie = setCookie.split(";", 2)[0];
            HttpRequest me = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8081/v1/me"))
                    .timeout(Duration.ofSeconds(10)).header("Cookie", cookie).GET().build();
            assertThat(client.send(me, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
            assertThat(sessionCount(alice)).isEqualTo(1);
            HttpResponse<String> logout = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8081/v1/auth/logout"))
                    .timeout(Duration.ofSeconds(10)).header("Cookie", cookie)
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(logout.statusCode()).isEqualTo(204);
            assertThat(logout.headers().firstValue("Set-Cookie").orElseThrow()).contains("Max-Age=0");
            assertThat(sessionCount(alice)).isZero();
            assertThat(client.send(me, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        }
    }

    @Test
    void loginByUsernameAndEmailPersistsSessionsAndLogoutRevokesOnlyItsOwnCookie() throws Exception {
        Cookie first = login(alice.username().toUpperCase());
        Cookie second = login(alice.email().toUpperCase());
        assertThat(first.getValue()).isNotEqualTo(second.getValue());
        assertThat(first.isHttpOnly()).isTrue();
        assertThat(first.getMaxAge()).isGreaterThan(0);
        assertThat(first.getAttribute("SameSite")).isEqualToIgnoringCase("Lax");
        assertThat(sessionCount(alice)).isEqualTo(2);
        assertThat(jdbc.queryForList("select max_inactive_interval from spring_session where principal_name=?",
                Integer.class, alice.username())).allMatch(interval -> interval < 0);
        http.perform(get("/v1/me").cookie(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alice.id()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        MvcResult logout = http.perform(post("/v1/auth/logout").cookie(first))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(logout.getResponse().getCookie("SESSION").getMaxAge()).isZero();
        assertThat(sessionCount(alice)).isEqualTo(1);
        assertUnavailable(get("/v1/me").cookie(first));
        http.perform(get("/v1/me").cookie(second)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from spring_session_attributes a left join spring_session s "
                + "on a.session_primary_id=s.primary_id where s.primary_id is null", Integer.class)).isZero();
    }

    @Test
    void wrongPasswordUnknownLoginAndInactiveAccountHaveTheSameLoginError() throws Exception {
        jdbc.update("update app_user set active=false where id=?", bob.id());
        JsonNode expected = failedLogin(alice.username(), "Wrong-fixture-123");
        JsonNode unknown = failedLogin("unknown_" + UUID.randomUUID(), PASSWORD);
        JsonNode inactive = failedLogin(bob.username(), PASSWORD);
        assertThat(unknown.get("title")).isEqualTo(expected.get("title"));
        assertThat(unknown.get("detail")).isEqualTo(expected.get("detail"));
        assertThat(inactive.get("title")).isEqualTo(expected.get("title"));
        assertThat(inactive.get("detail")).isEqualTo(expected.get("detail"));
        assertThat(sessionCount(alice)).isZero();
        assertThat(sessionCount(bob)).isZero();
    }

    @Test
    void missingAndInvalidSessionsReturnConsistentProblemsOnProtectedRoutes() throws Exception {
        for (String path : List.of("/v1/me", "/v1/categories", "/v1/me/favorites", "/v1/me/visits",
                "/v1/admin/users")) {
            assertUnavailable(get(path));
            assertUnavailable(get(path).cookie(new Cookie("SESSION", "not-a-session")));
        }
        assertUnavailable(put("/v1/me/favorites/" + physical));
        assertUnavailable(post("/v1/me/visits").contentType(MediaType.APPLICATION_JSON)
                .content(visit(physical, EVENT_TIME)));
    }

    @Test
    void collaboratorCannotAdministerAnyResourceButCanReadTheCatalog() throws Exception {
        Cookie collaborator = login(alice.username());
        for (String resource : List.of("users", "categories", "services", "benefits", "establishments")) {
            String path = "/v1/admin/" + resource;
            http.perform(get(path).cookie(collaborator)).andExpect(status().isForbidden());
            http.perform(post(path).cookie(collaborator).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            String id = resource.equals("users") ? bob.id().toString() : physical.toString();
            http.perform(patch(path + "/" + id).cookie(collaborator)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            http.perform(post(path + "/" + id + "/deactivation").cookie(collaborator))
                    .andExpect(status().isForbidden());
            if (!resource.equals("users")) {
                http.perform(post(path + "/batch").cookie(collaborator)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().isForbidden());
            }
        }
        http.perform(get("/v1/establishments/" + physical).cookie(collaborator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(physical.toString()));
        http.perform(get("/v1/admin/users").cookie(login(admin.username()))).andExpect(status().isOk());
    }

    @Test
    void adminCreationCommitsOnlyAfterGoogleAcceptsAndStoresOnlyThePasswordHash() throws Exception {
        Cookie administrator = login(admin.username());
        String username = "new_" + UUID.randomUUID().toString().replace("-", "");
        acceptMail();
        MvcResult created = http.perform(post("/v1/admin/users").cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON).content(newUser(username)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("COLLABORATOR"))
                .andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        assertThat(created.getResponse().getContentAsString()).doesNotContain(PASSWORD, "test-secret", "test-access");
        String hash = jdbc.queryForObject("select password_hash from app_user where username=?", String.class, username);
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        http.perform(get("/v1/me").cookie(login(username))).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void gmailRejectionRollsBackTheFlushedUserAndAllowsRetry() throws Exception {
        Cookie administrator = login(admin.username());
        String username = "retry_" + UUID.randomUUID().toString().replace("-", "");
        expectToken();
        gmail.server.expect(requestTo(SEND_URL)).andRespond(withBadRequest().body("private-provider-diagnostic"));
        MvcResult rejected = http.perform(post("/v1/admin/users").cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON).content(newUser(username)))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.status").value(502)).andReturn();
        assertThat(rejected.getResponse().getContentAsString())
                .doesNotContain(PASSWORD, "private-provider-diagnostic", "test-access", "test-secret");
        assertThat(jdbc.queryForObject("select count(*) from app_user where username=?", Integer.class, username)).isZero();
        gmail.server.verify();
        gmail.server.reset();
        acceptMail();
        http.perform(post("/v1/admin/users").cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON).content(newUser(username))).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from app_user where username=?", Integer.class, username)).isEqualTo(1);
    }

    @Test
    void deactivationDeletesAllTargetSessionsAndBlocksReadAndOfflineWrites() throws Exception {
        Cookie administrator = login(admin.username());
        Cookie first = login(alice.username());
        Cookie second = login(alice.email());
        Cookie other = login(bob.username());
        http.perform(put("/v1/me/favorites/" + physical).cookie(first)).andExpect(status().isNoContent());
        assertThat(sessionCount(alice)).isEqualTo(2);
        http.perform(post("/v1/admin/users/" + alice.id() + "/deactivation").cookie(administrator))
                .andExpect(status().isNoContent());
        assertThat(sessionCount(alice)).isZero();
        assertUnavailable(get("/v1/me").cookie(first));
        assertUnavailable(get("/v1/me/favorites").cookie(second));
        assertUnavailable(delete("/v1/me/favorites/" + physical).cookie(first));
        assertUnavailable(post("/v1/me/visits").cookie(second).contentType(MediaType.APPLICATION_JSON)
                .content(visit(physical, EVENT_TIME)));
        failedLogin(alice.username(), PASSWORD);
        assertThat(count("user_favorite", alice)).isEqualTo(1);
        assertThat(count("user_visit", alice)).isZero();
        http.perform(get("/v1/me").cookie(other)).andExpect(status().isOk());
        http.perform(get("/v1/admin/users/" + alice.id()).cookie(administrator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void editingAndPromotingAnAccountRevokesItsSessionsAndAdminRestrictionsRemain() throws Exception {
        Cookie administrator = login(admin.username());
        Cookie previous = login(alice.username());
        Cookie previousEmail = login(alice.email());
        String renamed = "renamed_" + UUID.randomUUID().toString().replace("-", "");
        http.perform(patch("/v1/admin/users/" + alice.id()).cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", renamed, "email", renamed + "@gmail.com", "role", "ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        assertThat(sessionCount(alice)).isZero();
        assertUnavailable(get("/v1/me").cookie(previous));
        assertUnavailable(get("/v1/me").cookie(previousEmail));
        failedLogin(alice.username(), PASSWORD);
        Cookie promoted = login(renamed + "@gmail.com");
        http.perform(get("/v1/admin/users").cookie(promoted)).andExpect(status().isOk());
        http.perform(patch("/v1/admin/users/" + alice.id()).cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"COLLABORATOR\"}"))
                .andExpect(status().isUnprocessableEntity());
        http.perform(post("/v1/admin/users/" + alice.id() + "/deactivation").cookie(administrator))
                .andExpect(status().isForbidden());
        http.perform(post("/v1/admin/users/" + admin.id() + "/deactivation").cookie(administrator))
                .andExpect(status().isForbidden());
        http.perform(patch("/v1/admin/users/" + bob.id()).cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Fixture-new-123\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void favoritesPersistIdempotentlyAndOneAccountCannotReadOrRemoveAnotherAccountsData() throws Exception {
        Cookie first = login(alice.username());
        Cookie other = login(bob.username());
        http.perform(put("/v1/me/favorites/" + physical).cookie(first)).andExpect(status().isNoContent());
        http.perform(put("/v1/me/favorites/" + physical).cookie(first)).andExpect(status().isNoContent());
        assertThat(count("user_favorite", alice)).isEqualTo(1);
        http.perform(get("/v1/me/favorites").cookie(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].establishmentId").value(physical.toString()));
        http.perform(get("/v1/me/favorites").cookie(other)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        http.perform(delete("/v1/me/favorites/" + physical).cookie(other)).andExpect(status().isNoContent());
        assertThat(count("user_favorite", alice)).isEqualTo(1);
        http.perform(put("/v1/me/favorites/" + physical).cookie(other)).andExpect(status().isNoContent());
        http.perform(delete("/v1/me/favorites/" + physical).cookie(first)).andExpect(status().isNoContent());
        http.perform(delete("/v1/me/favorites/" + physical).cookie(first)).andExpect(status().isNoContent());
        assertThat(count("user_favorite", alice)).isZero();
        assertThat(count("user_favorite", bob)).isEqualTo(1);
    }

    @Test
    void offlineVisitsUseUtcDeduplicateWithinThirtyMinutesAndKeepOwnersSeparate() throws Exception {
        Cookie first = login(alice.username());
        Cookie other = login(bob.username());
        JsonNode original = submitVisit(first, physical, EVENT_TIME, 201);
        String originalId = original.get("id").asText();
        assertThat(submitVisit(first, physical, "2020-01-02T09:00:00-03:00", 200).get("id").asText()).isEqualTo(originalId);
        assertThat(submitVisit(first, physical, "2020-01-02T12:30:00Z", 200).get("id").asText()).isEqualTo(originalId);
        assertThat(submitVisit(first, physical, "2020-01-02T11:45:00Z", 200).get("id").asText()).isEqualTo(originalId);
        submitVisit(first, physical, "2020-01-02T12:31:00Z", 201);
        http.perform(get("/v1/me/visits").cookie(other)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        submitVisit(other, physical, EVENT_TIME, 201);
        assertThat(count("user_visit", alice)).isEqualTo(2);
        assertThat(count("user_visit", bob)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select occurred_at from user_visit where id=?",
                java.sql.Timestamp.class, UUID.fromString(originalId)).toInstant()).isEqualTo(Instant.parse(EVENT_TIME));
        http.perform(get("/v1/me/visits?page=0&size=1").cookie(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].occurredAt").value("2020-01-02T12:31:00Z"))
                .andExpect(jsonPath("$.content[0].latitude").doesNotExist())
                .andExpect(jsonPath("$.content[0].longitude").doesNotExist());
    }

    @Test
    void simultaneousRetriesDoNotCreateDuplicateFavoritesOrVisits() throws Exception {
        Cookie session = login(alice.username());
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Integer>> favoriteRequests = java.util.stream.IntStream.range(0, 4)
                    .mapToObj(i -> (Callable<Integer>) () -> http.perform(
                            put("/v1/me/favorites/" + physical).cookie(session)).andReturn().getResponse().getStatus()).toList();
            for (var result : executor.invokeAll(favoriteRequests, 30, TimeUnit.SECONDS)) {
                assertThat(result.get()).isEqualTo(204);
            }
            List<Callable<MvcResult>> visitRequests = java.util.stream.IntStream.range(0, 4)
                    .mapToObj(i -> (Callable<MvcResult>) () -> http.perform(post("/v1/me/visits").cookie(session)
                            .contentType(MediaType.APPLICATION_JSON).content(visit(physical, EVENT_TIME)))
                            .andReturn()).toList();
            int creations = 0;
            for (var result : executor.invokeAll(visitRequests, 30, TimeUnit.SECONDS)) {
                int status = result.get().getResponse().getStatus();
                assertThat(status).isIn(200, 201);
                if (status == 201) creations++;
            }
            assertThat(creations).isEqualTo(1);
        }
        assertThat(count("user_favorite", alice)).isEqualTo(1);
        assertThat(count("user_visit", alice)).isEqualTo(1);
    }

    @Test
    void onlinePartnersAllowFavoritesButRejectVisitsAndInactivePhysicalPartnersAcceptOfflineVisits() throws Exception {
        Cookie session = login(alice.username());
        http.perform(put("/v1/me/favorites/" + online).cookie(session)).andExpect(status().isNoContent());
        http.perform(post("/v1/me/visits").cookie(session).contentType(MediaType.APPLICATION_JSON)
                .content(visit(online, EVENT_TIME))).andExpect(status().isBadRequest());
        catalog.deactivateEstablishment(physical);
        http.perform(put("/v1/me/favorites/" + physical).cookie(session)).andExpect(status().isNotFound());
        submitVisit(session, physical, EVENT_TIME, 201);
        assertThat(count("user_visit", alice)).isEqualTo(1);
    }

    @Test
    void accountDisabledAfterAuthenticationUsesTheSame401AsAnInvalidSession() throws Exception {
        Cookie stale = login(alice.username());
        // Simulate deactivation racing an already authenticated request, before session revocation.
        jdbc.update("update app_user set active=false where id=?", alice.id());
        assertUnavailable(put("/v1/me/favorites/" + physical).cookie(stale));
        assertUnavailable(post("/v1/me/visits").cookie(stale).contentType(MediaType.APPLICATION_JSON)
                .content(visit(physical, EVENT_TIME)));
        assertThat(count("user_favorite", alice)).isZero();
        assertThat(count("user_visit", alice)).isZero();
    }

    @Test
    void accountMissingAfterAuthenticationUsesTheSame401AsAnInvalidSession() throws Exception {
        Cookie stale = login(alice.username());
        jdbc.update("delete from app_user where id=?", alice.id());
        assertUnavailable(put("/v1/me/favorites/" + physical).cookie(stale));
    }

    private Account account(String prefix, String role, String hash) {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "");
        String email = username + "@gmail.com";
        Integer id = jdbc.queryForObject("insert into app_user(username,email,password_hash,role) values (?,?,?,?) returning id",
                Integer.class, username, email, hash, role);
        return new Account(id, username, email);
    }

    private UUID place(UUID category, UUID benefit, EstablishmentChannel channel) {
        boolean onlineOnly = channel == EstablishmentChannel.ONLINE;
        return catalog.createEstablishment(new EstablishmentCreateRequest("Parceiro " + UUID.randomUUID(),
                "Descrição de teste", onlineOnly ? null : "89200000", onlineOnly ? null : 100,
                onlineOnly ? null : "Rua Teste", onlineOnly ? null : "Centro", onlineOnly ? null : "Joinville",
                onlineOnly ? null : "SC", onlineOnly ? null : new BigDecimal("-26.3"),
                onlineOnly ? null : new BigDecimal("-48.8"), null, null, category, benefit, List.of(), channel)).id();
    }

    private Cookie login(String login) throws Exception {
        MvcResult result = http.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertThat(cookie).isNotNull();
        // Only the cookie travels to the next request: no MockHttpSession or mock Authentication.
        return cookie;
    }

    private JsonNode failedLogin(String login, String password) throws Exception {
        MvcResult result = http.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "password", password))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401)).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(password, "passwordHash", "stackTrace");
        return body(result);
    }

    private void assertUnavailable(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = http.perform(request.header("X-Request-Id", "integration-session-check"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Request-Id", "integration-session-check"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.detail").value("É necessário entrar para acessar este recurso."))
                .andExpect(jsonPath("$.requestId").value("integration-session-check")).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(
                PASSWORD, "passwordHash", "stackTrace", "Conta indisponível", "Conta inválida");
    }

    private String newUser(String username) throws Exception {
        return json.writeValueAsString(Map.of("username", username, "email", username + "@gmail.com", "password", PASSWORD));
    }

    private String visit(UUID place, String occurredAt) throws Exception {
        return json.writeValueAsString(Map.of("establishmentId", place.toString(), "occurredAt", occurredAt));
    }

    private JsonNode submitVisit(Cookie cookie, UUID place, String occurredAt, int expected) throws Exception {
        return body(http.perform(post("/v1/me/visits").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content(visit(place, occurredAt))).andExpect(status().is(expected)).andReturn());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }

    private int sessionCount(Account account) {
        return jdbc.queryForObject("select count(*) from spring_session where principal_name=?",
                Integer.class, account.username());
    }

    private int count(String table, Account account) {
        // Table names only come from constants in this test class.
        return jdbc.queryForObject("select count(*) from " + table + " where user_id=?", Integer.class, account.id());
    }

    private void expectToken() {
        gmail.server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"test-access\"}", MediaType.APPLICATION_JSON));
    }

    private void acceptMail() {
        expectToken();
        gmail.server.expect(requestTo(SEND_URL)).andRespond(withSuccess("{\"id\":\"test-message\"}", MediaType.APPLICATION_JSON));
    }

    private record Account(Integer id, String username, String email) { }

    static class GmailStub {
        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MailConfiguration {
        @Bean GmailStub gmailStub() { return new GmailStub(); }
        @Bean @Primary RestClient.Builder isolatedMailBuilder(GmailStub gmail) { return gmail.builder; }
    }
}
