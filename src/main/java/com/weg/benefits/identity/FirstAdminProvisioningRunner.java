package com.weg.benefits.identity;

import java.io.Console;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.cli.provision-admin", havingValue = "true")
public class FirstAdminProvisioningRunner implements ApplicationRunner {
    private final IdentityService identityService;

    public FirstAdminProvisioningRunner(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Provisionamento exige terminal interativo; nenhum dado foi coletado.");
        }
        String username = console.readLine("Username do primeiro ADMIN: ");
        String email = console.readLine("E-mail do primeiro ADMIN: ");
        char[] passwordChars = console.readPassword("Senha (6 a 72 caracteres): ");
        if (passwordChars == null) {
            throw new IllegalStateException("Leitura da senha cancelada; nenhum dado foi salvo.");
        }
        try {
            identityService.provisionFirstAdmin(username, email, new String(passwordChars));
            console.printf("Primeiro ADMIN provisionado e mensagem aceita pelo SMTP.%n");
        } finally {
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }
}
