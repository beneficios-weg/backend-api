package com.weg.benefits.catalog;

import org.springframework.http.HttpStatus;

public final class CatalogException extends RuntimeException {
    private final HttpStatus status;
    private final String title;

    CatalogException(HttpStatus status, String title, String message) {
        super(message);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() { return status; }
    public String getTitle() { return title; }

    static CatalogException notFound(String resource) {
        return new CatalogException(HttpStatus.NOT_FOUND, "Recurso não encontrado", resource + " não encontrado.");
    }

    static CatalogException invalid(String message) {
        return new CatalogException(HttpStatus.BAD_REQUEST, "Requisição inválida", message);
    }

    static CatalogException conflict(String message) {
        return new CatalogException(HttpStatus.CONFLICT, "Conflito", message);
    }
}
