package com.weg.benefits.catalog;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.weg.benefits.catalog.CatalogDtos.BenefitCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.BenefitBatchCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.BenefitPatchRequest;
import com.weg.benefits.catalog.CatalogDtos.BenefitResponse;
import com.weg.benefits.catalog.CatalogDtos.CategoryCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.CategoryBatchCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.CategoryOptionsResponse;
import com.weg.benefits.catalog.CatalogDtos.CategoryPatchRequest;
import com.weg.benefits.catalog.CatalogDtos.CategoryResponse;
import com.weg.benefits.catalog.CatalogDtos.DiscountInput;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentBatchCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentPatchRequest;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentResponse;
import com.weg.benefits.catalog.CatalogDtos.PageResponse;
import com.weg.benefits.catalog.CatalogDtos.ServiceCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.ServiceBatchCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.ServicePatchRequest;
import com.weg.benefits.catalog.CatalogDtos.ServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {
    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categories;
    private final ServiceOptionRepository services;
    private final EstablishmentRepository establishments;
    private final BenefitRepository benefits;

    public CatalogService(CategoryRepository categories, ServiceOptionRepository services,
            EstablishmentRepository establishments, BenefitRepository benefits) {
        this.categories = categories;
        this.services = services;
        this.establishments = establishments;
        this.benefits = benefits;
    }

    @Transactional(readOnly = true)
    public CategoryOptionsResponse categoryOptions() {
        return new CategoryOptionsResponse(CategoryPalette.colors(), "Iconify");
    }

    public CategoryResponse createCategory(CategoryCreateRequest request) {
        validateLength(request.name(), 5, 75, "nome da categoria");
        requirePaletteColor(request.colorHex());
        return CategoryResponse.from(categories.save(new Category(request.name().trim(), request.iconKey(),
                request.colorHex().toUpperCase(java.util.Locale.ROOT))));
    }

    public List<CategoryResponse> createCategories(CategoryBatchCreateRequest request) {
        return request.categories().stream().map(this::createCategory).toList();
    }

    public CategoryResponse updateCategory(UUID id, CategoryPatchRequest request) {
        requireChanges(request.hasChanges());
        if (request.name() != null) validateLength(request.name(), 5, 75, "nome da categoria");
        Category value = category(id);
        if (request.colorHex() != null) requirePaletteColor(request.colorHex());
        value.update(trim(request.name()), request.iconKey(), upper(request.colorHex()), request.active());
        return CategoryResponse.from(value);
    }

    public void deactivateCategory(UUID id) { category(id).deactivate(); }

    public PageResponse<CategoryResponse> adminCategories(int page, int size) {
        return map(categories.findAllByOrderByIdAsc(pageable(page, size)), CategoryResponse::from);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID id) { return CategoryResponse.from(category(id)); }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> activeCategories(int page, int size) {
        return map(categories.findAllByActiveTrueOrderByNameAsc(pageable(page, size)), CategoryResponse::from);
    }

    public ServiceResponse createService(ServiceCreateRequest request) {
        if (services.existsByNameIgnoreCase(request.name().trim())) {
            throw CatalogException.conflict("Já existe um serviço com esse nome.");
        }
        return ServiceResponse.from(services.save(new ServiceOption(request.name().trim())));
    }

    public List<ServiceResponse> createServices(ServiceBatchCreateRequest request) {
        Set<String> normalizedNames = new HashSet<>();
        for (ServiceCreateRequest item : request.services()) {
            String normalized = item.name().trim().toLowerCase(java.util.Locale.ROOT);
            if (!normalizedNames.add(normalized)) {
                throw CatalogException.conflict("O lote contém serviços com nomes repetidos.");
            }
        }
        return request.services().stream().map(this::createService).toList();
    }

    public ServiceResponse updateService(UUID id, ServicePatchRequest request) {
        requireChanges(request.hasChanges());
        ServiceOption value = service(id);
        if (request.name() != null && services.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw CatalogException.conflict("Já existe um serviço com esse nome.");
        }
        value.update(trim(request.name()), request.active());
        return ServiceResponse.from(value);
    }

    public void deactivateService(UUID id) { service(id).update(null, false); }

    public PageResponse<ServiceResponse> adminServices(int page, int size) {
        return map(services.findAllByOrderByIdAsc(pageable(page, size)), ServiceResponse::from);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(UUID id) { return ServiceResponse.from(service(id)); }

    @Transactional(readOnly = true)
    public PageResponse<ServiceResponse> activeServices(int page, int size) {
        return map(services.findAllByActiveTrueOrderByNameAsc(pageable(page, size)), ServiceResponse::from);
    }

    public EstablishmentResponse createEstablishment(EstablishmentCreateRequest request) {
        validateLength(request.name(), 2, Integer.MAX_VALUE, "nome");
        validateRequiredText(request.description(), "descrição");
        EstablishmentChannel channel = request.channel() == null ? EstablishmentChannel.PHYSICAL : request.channel();
        EstablishmentLocation.from(request).validate(channel);
        String imageUrl = canonicalImageUrl(request.facadeImageUrl());
        Category category = activeCategory(request.categoryId());
        Set<ServiceOption> linkedServices = activeServices(request.serviceIds());
        Establishment value = new Establishment(request.name(), request.description(),
                request.postalCode(), request.streetNumber(), trim(request.street()), trim(request.district()),
                trim(request.city()), upper(request.state()), request.latitude(), request.longitude(),
                request.altitude(), imageUrl, category, linkedServices, channel);
        assignBenefit(value, request.benefitId());
        return EstablishmentResponse.from(establishments.save(value));
    }

    public List<EstablishmentResponse> createEstablishments(EstablishmentBatchCreateRequest request) {
        return request.establishments().stream().map(this::createEstablishment).toList();
    }

    public EstablishmentResponse updateEstablishment(UUID id, EstablishmentPatchRequest request) {
        requireChanges(request.hasChanges());
        if (request.name() != null) validateLength(request.name(), 2, Integer.MAX_VALUE, "nome");
        validateOptionalText(request.description(), "descrição");
        if (Boolean.TRUE.equals(request.clearFacadeImageUrl()) && request.facadeImageUrl() != null) {
            throw CatalogException.invalid("Informe a URL da fachada ou solicite sua remoção, não ambos.");
        }
        String imageUrl = canonicalImageUrl(request.facadeImageUrl());
        Establishment value = establishment(id);
        EstablishmentChannel channel = request.channel() == null ? value.getChannel() : request.channel();
        EstablishmentLocation.from(request, value, channel).validate(channel);
        Category category = request.categoryId() == null ? null : activeCategory(request.categoryId());
        Set<ServiceOption> linkedServices = request.serviceIds() == null ? null : activeServices(request.serviceIds());
        if (request.benefitId() != null) assignBenefit(value, request.benefitId());
        value.update(request.name(), request.description(), request.postalCode(), request.streetNumber(),
                trim(request.street()), trim(request.district()), trim(request.city()), upper(request.state()),
                request.latitude(), request.longitude(), request.altitude(), imageUrl, category,
                linkedServices, request.active(), channel);
        if (Boolean.TRUE.equals(request.clearFacadeImageUrl())) value.clearFacadeImageUrl();
        return EstablishmentResponse.from(value);
    }

    public void deactivateEstablishment(UUID id) { establishment(id).deactivate(); }

    @Transactional(readOnly = true)
    public PageResponse<EstablishmentResponse> adminEstablishments(int page, int size) {
        return map(establishments.findAllByOrderByIdAsc(pageable(page, size)), EstablishmentResponse::from);
    }

    @Transactional(readOnly = true)
    public EstablishmentResponse getAdminEstablishment(UUID id) {
        return EstablishmentResponse.from(establishment(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<EstablishmentResponse> searchEstablishments(String query, UUID categoryId,
            UUID serviceId, EstablishmentChannel channel, boolean forMap, int page, int size) {
        if (query != null && query.length() > 100) throw CatalogException.invalid("A busca aceita até 100 caracteres.");
        String normalized = query == null || query.isBlank() ? "" : query.trim();
        return map(establishments.searchActive(normalized, categoryId, serviceId, channel, forMap,
                pageable(page, Math.min(size, 50))), EstablishmentResponse::from);
    }

    @Transactional(readOnly = true)
    public EstablishmentResponse getActiveEstablishment(UUID id) {
        return EstablishmentResponse.from(establishments.findActiveById(id)
                .orElseThrow(() -> CatalogException.notFound("Estabelecimento")));
    }

    @Transactional(readOnly = true)
    public Establishment requireActiveEstablishmentForEngagement(UUID id) {
        return establishments.findActiveById(id)
                .orElseThrow(() -> CatalogException.notFound("Estabelecimento"));
    }

    @Transactional(readOnly = true)
    public Establishment requireExistingEstablishmentForEngagement(UUID id) {
        return establishment(id);
    }

    public BenefitResponse createBenefit(BenefitCreateRequest request) {
        validateRequiredText(request.audience(), "público");
        validateRequiredText(request.participationInstructions(), "instruções de participação");
        validateRequiredText(request.contact(), "contato");
        validateDiscounts(request.discounts());
        return BenefitResponse.from(benefits.save(new Benefit(request.audience(),
                request.participationInstructions(), request.contact(),
                discounts(request.discounts()))));
    }

    public List<BenefitResponse> createBenefits(BenefitBatchCreateRequest request) {
        return request.benefits().stream().map(this::createBenefit).toList();
    }

    public BenefitResponse updateBenefit(UUID id, BenefitPatchRequest request) {
        requireChanges(request.hasChanges());
        validateOptionalText(request.audience(), "público");
        validateOptionalText(request.contact(), "contato");
        validateOptionalText(request.participationInstructions(), "instruções de participação");
        if (request.discounts() != null) validateDiscounts(request.discounts());
        Benefit value = benefit(id);
        value.update(request.audience(), request.participationInstructions(), request.contact(),
                request.discounts() == null ? null : discounts(request.discounts()), request.active());
        return BenefitResponse.from(value);
    }

    public void deactivateBenefit(UUID id) { benefit(id).deactivate(); }

    @Transactional(readOnly = true)
    public PageResponse<BenefitResponse> adminBenefits(int page, int size) {
        return map(benefits.findAllByOrderByIdAsc(pageable(page, size)), BenefitResponse::from);
    }

    @Transactional(readOnly = true)
    public BenefitResponse getBenefit(UUID id) { return BenefitResponse.from(benefit(id)); }

    @Transactional(readOnly = true)
    public PageResponse<BenefitResponse> activeBenefits(UUID establishmentId, int page, int size) {
        if (establishmentId == null) throw CatalogException.invalid("Informe o estabelecimento.");
        Page<Benefit> results = benefits.findActiveByEstablishment(
                establishmentId, pageable(page, size));
        return map(results, BenefitResponse::from);
    }

    private Category category(UUID id) {
        return categories.findById(id).orElseThrow(() -> CatalogException.notFound("Categoria"));
    }
    private Category activeCategory(UUID id) {
        Category value = category(id);
        if (!value.isActive()) throw CatalogException.invalid("A categoria selecionada está inativa.");
        return value;
    }
    private ServiceOption service(UUID id) {
        return services.findById(id).orElseThrow(() -> CatalogException.notFound("Serviço"));
    }
    private Establishment establishment(UUID id) {
        return establishments.findById(id).orElseThrow(() -> CatalogException.notFound("Estabelecimento"));
    }
    private Benefit benefit(UUID id) {
        return benefits.findById(id).orElseThrow(() -> CatalogException.notFound("Benefício"));
    }
    private void assignBenefit(Establishment establishment, UUID benefitId) {
        Benefit selected = benefits.findByIdForUpdate(benefitId)
                .orElseThrow(() -> CatalogException.notFound("Benefício"));
        if (selected.getDiscounts().isEmpty()) {
            throw CatalogException.invalid("O benefício selecionado precisa ter ao menos um desconto.");
        }
        Benefit previous = establishment.getBenefit();
        if (previous != null && previous != selected) previous.unlinkFrom(establishment);
        selected.linkTo(establishment);
    }
    private static List<Discount> discounts(List<DiscountInput> values) {
        return values.stream().map(value -> new Discount(value.value(), value.description())).toList();
    }
    private static void validateDiscounts(List<DiscountInput> values) {
        if (values == null || values.isEmpty()) {
            throw CatalogException.invalid("O benefício deve possuir ao menos um desconto.");
        }
        for (DiscountInput value : values) {
            if (value == null) throw CatalogException.invalid("Informe um desconto válido.");
            validateLength(value.value(), 2, 15, "valor do desconto");
            validateRequiredText(value.description(), "descrição do desconto");
        }
    }
    private Set<ServiceOption> activeServices(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        Set<UUID> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) throw CatalogException.invalid("A lista de serviços contém IDs repetidos.");
        List<ServiceOption> found = services.findAllById(uniqueIds);
        if (found.size() != uniqueIds.size() || found.stream().anyMatch(value -> !value.isActive())) {
            throw CatalogException.invalid("Um ou mais serviços não existem ou estão inativos.");
        }
        return Set.copyOf(found);
    }
    private static String canonicalImageUrl(String value) {
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException();
            return value.replaceFirst("(?i)^https://", "https://");
        } catch (IllegalArgumentException exception) {
            throw CatalogException.invalid("A imagem de fachada deve usar uma URL HTTPS válida.");
        }
    }
    private static void requirePaletteColor(String color) {
        if (!CategoryPalette.containsHex(color)) throw CatalogException.invalid("A cor deve pertencer à paleta aprovada.");
    }
    private static void requireChanges(boolean value) {
        if (!value) throw CatalogException.invalid("Informe ao menos um campo para alteração.");
    }
    private static void validateOptionalText(String value, String field) {
        if (value != null && value.isBlank()) throw CatalogException.invalid("O campo " + field + " é obrigatório.");
    }
    private static void validateRequiredText(String value, String field) {
        if (value == null || value.isBlank()) throw CatalogException.invalid("O campo " + field + " é obrigatório.");
    }
    private static void validateLength(String value, int min, int max, String field) {
        if (value == null || value.trim().length() < min || value.trim().length() > max) {
            throw CatalogException.invalid("O campo " + field + " deve ter entre " + min + " e " + max + " caracteres.");
        }
    }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String upper(String value) { return value == null ? null : value.toUpperCase(java.util.Locale.ROOT); }
    private static PageRequest pageable(int page, int size) {
        if (page < 0 || size < 1) throw CatalogException.invalid("Página ou tamanho inválido.");
        if (size > MAX_PAGE_SIZE) throw CatalogException.invalid("O tamanho máximo da página é " + MAX_PAGE_SIZE + ".");
        return PageRequest.of(page, size, Sort.unsorted());
    }
    private static <S, T> PageResponse<T> map(Page<S> page, java.util.function.Function<S, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
