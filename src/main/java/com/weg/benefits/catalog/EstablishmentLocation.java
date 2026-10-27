package com.weg.benefits.catalog;

import java.math.BigDecimal;

/** Validates the complete effective location, including fields omitted by a PATCH. */
record EstablishmentLocation(String postalCode, Integer streetNumber, String street,
        String district, String city, String state, BigDecimal latitude,
        BigDecimal longitude, BigDecimal altitude) {

    static EstablishmentLocation from(CatalogDtos.EstablishmentCreateRequest request) {
        return new EstablishmentLocation(request.postalCode(), request.streetNumber(), request.street(),
                request.district(), request.city(), request.state(), request.latitude(),
                request.longitude(), request.altitude());
    }

    static EstablishmentLocation from(CatalogDtos.EstablishmentPatchRequest request,
            Establishment current, EstablishmentChannel channel) {
        // A switch to ONLINE deliberately clears the old physical location, atomically.
        if (channel == EstablishmentChannel.ONLINE) {
            return new EstablishmentLocation(request.postalCode(), request.streetNumber(), request.street(),
                    request.district(), request.city(), request.state(), request.latitude(),
                    request.longitude(), request.altitude());
        }
        return new EstablishmentLocation(orCurrent(request.postalCode(), current.getPostalCode()),
                orCurrent(request.streetNumber(), current.getStreetNumber()),
                orCurrent(request.street(), current.getStreet()), orCurrent(request.district(), current.getDistrict()),
                orCurrent(request.city(), current.getCity()), orCurrent(request.state(), current.getState()),
                orCurrent(request.latitude(), current.getLatitude()), orCurrent(request.longitude(), current.getLongitude()),
                orCurrent(request.altitude(), current.getAltitude()));
    }

    void validate(EstablishmentChannel channel) {
        if (channel == EstablishmentChannel.ONLINE) {
            if (postalCode != null || streetNumber != null || street != null || district != null
                    || city != null || state != null || latitude != null || longitude != null || altitude != null) {
                throw CatalogException.invalid("Parceiro exclusivamente online não deve informar endereço ou coordenadas.");
            }
        } else if (blank(postalCode) || streetNumber == null || blank(street) || blank(district)
                || blank(city) || blank(state) || latitude == null || longitude == null) {
            throw CatalogException.invalid("Parceiro físico ou híbrido exige endereço completo e coordenadas.");
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static <T> T orCurrent(T supplied, T current) { return supplied == null ? current : supplied; }
}
