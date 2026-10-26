package com.weg.benefits.identity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CredentialMailSender {
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String SEND_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String sender;

    public CredentialMailSender(RestClient.Builder restClientBuilder,
            @Value("${app.mail.google.client-id:}") String clientId,
            @Value("${app.mail.google.client-secret:}") String clientSecret,
            @Value("${app.mail.google.refresh-token:}") String refreshToken,
            @Value("${app.mail.google.sender:wegbenefits@gmail.com}") String sender) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.sender = sender;
    }

    public void sendCredentials(String email, String username, String password) {
        try {
            String accessToken = refreshAccessToken();
            String rawMessage = encodeRawMessage(email, username, password);
            restClient.post()
                    .uri(SEND_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GmailSendRequest(rawMessage))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | IllegalStateException exception) {
            throw new IdentityException(HttpStatus.BAD_GATEWAY,
                    "Falha no envio de credenciais",
                    "O serviço de e-mail não aceitou a mensagem. A conta não foi criada; tente novamente.");
        }
    }

    private String refreshAccessToken() {
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            throw new IllegalStateException("Gmail API OAuth is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        TokenResponse response = restClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Google returned no access token");
        }
        return response.accessToken();
    }

    private String encodeRawMessage(String email, String username, String password) {
        if (email == null || !email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?:gmail\\.com|estudante\\.sesisenai\\.org\\.br)")) {
            throw new IllegalStateException("Invalid recipient address");
        }
        if (sender == null || !sender.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?:gmail\\.com|estudante\\.sesisenai\\.org\\.br)")) {
            throw new IllegalStateException("Invalid sender address");
        }

        String body = "Seu acesso ao WEG Benefits foi criado.\n\nUsuário: " + username
                + "\nSenha: " + password + "\n\nGuarde estas credenciais em local seguro.";
        String mimeMessage = "From: " + sender + "\r\n"
                + "To: " + email + "\r\n"
                + "Subject: Acesso ao WEG Benefits\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mimeMessage.getBytes(StandardCharsets.UTF_8));
    }

    private record TokenResponse(String access_token) {
        String accessToken() {
            return access_token;
        }
    }

    private record GmailSendRequest(String raw) { }
}
