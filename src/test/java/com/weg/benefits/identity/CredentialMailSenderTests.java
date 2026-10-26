package com.weg.benefits.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CredentialMailSenderTests {
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String SEND_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    @Test
    void refreshesTokenAndSendsCredentialMessageThroughGmailApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CredentialMailSender sender = new CredentialMailSender(builder,
                "client-id", "client-secret", "refresh-token", "wegbenefits@gmail.com");

        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("client_id=client-id"),
                        org.hamcrest.Matchers.containsString("client_secret=client-secret"),
                        org.hamcrest.Matchers.containsString("refresh_token=refresh-token"),
                        org.hamcrest.Matchers.containsString("grant_type=refresh_token"))))
                .andRespond(withSuccess("{\"access_token\":\"access-token\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(SEND_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(request -> assertCredentialPayload(((MockClientHttpRequest) request).getBodyAsBytes()))
                .andRespond(withSuccess("{\"id\":\"gmail-message-id\"}", MediaType.APPLICATION_JSON));

        sender.sendCredentials("colaborador@gmail.com", "colaborador", "senha-secreta");

        server.verify();
    }

    @Test
    void hidesGoogleAuthenticationErrorsFromApiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CredentialMailSender sender = new CredentialMailSender(builder,
                "client-id", "client-secret", "refresh-token-sensivel", "wegbenefits@gmail.com");
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withBadRequest().body("sensitive-provider-error"));

        assertThatThrownBy(() -> sender.sendCredentials("colaborador@gmail.com", "colaborador", "senha"))
                .isInstanceOf(IdentityException.class)
                .hasMessage("O serviço de e-mail não aceitou a mensagem. A conta não foi criada; tente novamente.")
                .hasNoCause();

        server.verify();
    }

    private static void assertCredentialPayload(byte[] requestBody) throws IOException {
        String json = new String(requestBody, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\\\"raw\\\":\\\"([^\\\"]+)\\\"").matcher(json);
        assertThat(matcher.find()).isTrue();

        String mimeMessage = new String(Base64.getUrlDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8);
        assertThat(mimeMessage).contains("From: wegbenefits@gmail.com", "To: colaborador@gmail.com",
                "Subject: Acesso ao WEG Benefits", "Content-Type: text/plain; charset=UTF-8");
        String encodedBody = mimeMessage.substring(mimeMessage.indexOf("\r\n\r\n") + 4).replace("\r\n", "");
        String body = new String(Base64.getMimeDecoder().decode(encodedBody), StandardCharsets.UTF_8);
        assertThat(body).contains("Usuário: colaborador", "Senha: senha-secreta");
    }
}
