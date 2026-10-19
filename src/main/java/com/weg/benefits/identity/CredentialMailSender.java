package com.weg.benefits.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class CredentialMailSender {
    private final JavaMailSender mailSender;
    private final String sender;

    public CredentialMailSender(JavaMailSender mailSender,
            @Value("${app.mail.from}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    public void sendCredentials(String email, String username, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Acesso ao WEG Benefits");
        message.setText("Seu acesso ao WEG Benefits foi criado.\n\nUsuário: " + username
                + "\nSenha: " + password + "\n\nGuarde estas credenciais em local seguro.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new IdentityException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Falha no envio de credenciais",
                    "O serviço de e-mail não aceitou a mensagem. A conta não foi criada; tente novamente.");
        }
    }
}
