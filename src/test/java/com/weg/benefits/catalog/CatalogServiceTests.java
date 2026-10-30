package com.weg.benefits.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.weg.benefits.catalog.CatalogDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogServiceTests {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ServiceOptionRepository services = mock(ServiceOptionRepository.class);
    private final EstablishmentRepository establishments = mock(EstablishmentRepository.class);
    private final BenefitRepository benefits = mock(BenefitRepository.class);
    private final CatalogService catalog = new CatalogService(categories, services, establishments, benefits);
    private final UUID categoryId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();

    @BeforeEach
    void repositoriesReturnSavedEntities() {
        when(categories.findById(categoryId)).thenReturn(Optional.of(new Category("Categoria", "mdi:store", "#0D47A1")));
        when(benefits.save(any(Benefit.class))).thenAnswer(call -> call.getArgument(0));
        when(establishments.save(any(Establishment.class))).thenAnswer(call -> call.getArgument(0));
        when(benefits.findByIdForUpdate(benefitId)).thenReturn(Optional.of(
                new Benefit("Público", "Apresentar crachá", "Contato", List.of(new Discount("20%", "20%")))));
    }

    @Test
    void categoryNameAcceptsLazerAndValidatesTheTrimmedLength() {
        when(categories.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
        assertEquals("Lazer", catalog.createCategory(new CategoryCreateRequest(" Lazer ", "lucide:ticket", "#E65100")).name());
        assertEquals("Lazer", catalog.updateCategory(categoryId, new CategoryPatchRequest(" Lazer ", null, null, null)).name());
        assertThrows(CatalogException.class, () -> catalog.createCategory(new CategoryCreateRequest(" Laze ", "lucide:ticket", "#E65100")));
        assertThrows(CatalogException.class, () -> catalog.updateCategory(categoryId, new CategoryPatchRequest("    ", null, null, null)));
    }

    @Test
    void sourceTextIsNotTrimmedRewrittenOrPaddedOnCreateAndPatch() {
        var created = catalog.createBenefit(new BenefitCreateRequest(" Público ", " Aapresentar crachá ",
                " Contato ", List.of(new DiscountInput("20%", " 20% "))));
        assertEquals(" Aapresentar crachá ", created.participationInstructions());
        assertEquals(" 20% ", created.discounts().getFirst().description());
        assertEquals(" Público ", created.audience());
        var stored = new Benefit("Público", "Antes", "Contato", List.of(new Discount("20%", "20%")));
        when(benefits.findById(benefitId)).thenReturn(Optional.of(stored));
        String longText = "Texto original, sem correções. ".repeat(30);
        var patched = catalog.updateBenefit(benefitId, new BenefitPatchRequest(null, longText, null,
                List.of(new DiscountInput("20%", longText)), null));
        assertEquals(longText, patched.participationInstructions());
        assertEquals(longText, patched.discounts().getFirst().description());
    }

    @Test
    void sourceTextStillCannotBeBlank() {
        assertThrows(CatalogException.class, () -> catalog.createBenefit(new BenefitCreateRequest(
                "Público", "  ", "Contato", List.of(new DiscountInput("20%", "20%")))));
        assertThrows(CatalogException.class, () -> catalog.createBenefit(new BenefitCreateRequest(
                "Público", "Crachá", "Contato", List.of(new DiscountInput("20%", "\t")))));
    }

    @Test
    void onlinePartnerHasNoLocationAndActivatesBenefitOnItsFirstLink() {
        var created = catalog.createEstablishment(online(EstablishmentChannel.ONLINE));
        assertEquals(EstablishmentChannel.ONLINE, created.channel());
        assertNull(created.latitude());
        assertNull(created.postalCode());
        assertFalse(created.mappable());
        assertTrue(benefits.findByIdForUpdate(benefitId).orElseThrow().isActive());
    }

    @Test
    void omittedChannelIsPhysicalAndRequiresLocation() {
        assertThrows(CatalogException.class, () -> catalog.createEstablishment(online(null)));
        assertThrows(CatalogException.class, () -> catalog.createEstablishment(online(EstablishmentChannel.HYBRID)));
        verify(establishments, never()).save(any());
    }

    @Test
    void switchingToOnlineClearsLocationAndSwitchingBackRequiresCompleteAddress() {
        Establishment current = new Establishment("Parceiro", "Descrição", "89200000", 100,
                "Rua Teste", "Centro", "Joinville", "SC", new BigDecimal("-26.3"),
                new BigDecimal("-48.8"), null, null, new Category("Categoria", "mdi:store", "#0D47A1"),
                java.util.Set.of(), EstablishmentChannel.PHYSICAL);
        UUID id = UUID.randomUUID();
        when(establishments.findById(id)).thenReturn(Optional.of(current));
        var changed = catalog.updateEstablishment(id, channelPatch(EstablishmentChannel.ONLINE));
        assertNull(changed.street());
        assertNull(changed.latitude());
        assertNull(changed.longitude());
        assertFalse(changed.mappable());
        assertThrows(CatalogException.class, () -> catalog.updateEstablishment(id,
                channelPatch(EstablishmentChannel.PHYSICAL)));
        assertEquals(EstablishmentChannel.ONLINE, current.getChannel());
    }

    @Test
    void onlineLocationCannotBeInvented() {
        var request = new EstablishmentCreateRequest("Parceiro", "Descrição", null, null, null, null,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, categoryId, benefitId,
                List.of(), EstablishmentChannel.ONLINE);
        assertThrows(CatalogException.class, () -> catalog.createEstablishment(request));
        verifyNoInteractions(establishments);
    }

    private EstablishmentCreateRequest online(EstablishmentChannel channel) {
        return new EstablishmentCreateRequest("Parceiro", "20%", null, null, null, null, null, null,
                null, null, null, null, categoryId, benefitId, List.of(), channel);
    }

    private EstablishmentPatchRequest channelPatch(EstablishmentChannel channel) {
        return new EstablishmentPatchRequest(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, channel);
    }
}
