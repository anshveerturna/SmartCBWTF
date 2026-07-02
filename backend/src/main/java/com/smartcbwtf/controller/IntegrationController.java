package com.smartcbwtf.controller;

import com.smartcbwtf.config.JwtAuthFilter;
import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AuditLog;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AuditLogRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.VehicleRepository;
import com.smartcbwtf.service.OAuthScopeRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

@RestController
@RequestMapping("/api/integration")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN','HCF_ADMIN','TOP_MANAGEMENT')")
public class IntegrationController {
    private static final int MAX_ENTITY_TYPE_LENGTH = 80;
    private static final String UNKNOWN_AUTHORIZATION = "permitAll or security-chain protected";
    private static final Pattern QUOTED_ROLE_PATTERN = Pattern.compile("'([^']+)'");

    @Value("${app.environment:${spring.profiles.active:local}}")
    private String environment = "local";

    private final RequestMappingHandlerMapping handlerMapping;
    private final OAuthScopeRegistry scopeRegistry;
    private final AppUserRepository appUserRepository;
    private final FacilityRepository facilityRepository;
    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final BagLabelRepository bagLabelRepository;
    private final AuditLogRepository auditLogRepository;

    public IntegrationController(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            OAuthScopeRegistry scopeRegistry,
            AppUserRepository appUserRepository, FacilityRepository facilityRepository, HcfRepository hcfRepository,
            AgreementRepository agreementRepository, VehicleRepository vehicleRepository, RouteRepository routeRepository,
            BagLabelRepository bagLabelRepository, AuditLogRepository auditLogRepository) {
        this.handlerMapping = handlerMapping;
        this.scopeRegistry = scopeRegistry;
        this.appUserRepository = appUserRepository;
        this.facilityRepository = facilityRepository;
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.bagLabelRepository = bagLabelRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/probe")
    public Map<String, Object> probe(HttpServletRequest request) {
        TenantContext.TenantInfo tenant = TenantContext.get();
        var user = appUserRepository.findByUsername(TenantContext.getUsername()).orElseThrow();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ready");
        response.put("provider", "smartcbwtf");
        response.put("apiVersion", "v1");
        response.put("connectedUser", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()));
        response.put("connection", connectionMetadata(request, user.getFacility(), user.getHcf()));
        response.put("grantedScopes", request.getAttribute(JwtAuthFilter.ATTR_SCOPES));
        response.put("requiredHeaders", List.of(
                "Authorization: Bearer <token>",
                "Idempotency-Key for POST/PUT/PATCH/DELETE",
                "X-AgentAI-Run-Id",
                "X-AgentAI-Workflow-Id",
                "X-AgentAI-Node-Id",
                "X-AgentAI-Reason"));
        response.put("readiness", Map.of(
                "oauthTokenValid", true,
                "tenantBound", tenant != null && tenant.tenantId() != null,
                "hcfBound", tenant != null && tenant.hcfId() != null,
                "roleScoped", user.getRole()));
        response.put("counts", counts(user.getRole(), tenant));
        response.put("documentation", Map.of(
                "openapiJson", "/v3/api-docs",
                "swaggerUi", "/swagger-ui/index.html",
                "endpointCatalog", "/api/integration/catalog/endpoints",
                "scopeMatrix", "/api/integration/scopes"));
        return response;
    }

    @GetMapping("/catalog/endpoints")
    public List<EndpointDescriptor> endpoints(HttpServletRequest request) {
        String role = TenantContext.getRole();
        boolean oauthAccessToken = JwtAuthFilter.TOKEN_USE_OAUTH_ACCESS.equals(
                request.getAttribute(JwtAuthFilter.ATTR_TOKEN_USE));
        var grantedScopes = scopeRegistry.splitScopes((String) request.getAttribute(JwtAuthFilter.ATTR_SCOPES));
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .flatMap(entry -> descriptors(entry.getKey(), entry.getValue()).stream())
                .filter(descriptor -> isCatalogEndpointVisibleForRole(
                        descriptor.path(), descriptor.authorization(), role))
                .filter(descriptor -> !oauthAccessToken
                        || descriptor.requiredOAuthScope() == null
                        || grantedScopes.contains(descriptor.requiredOAuthScope()))
                .sorted(Comparator.comparing(EndpointDescriptor::path).thenComparing(EndpointDescriptor::method))
                .toList();
    }

    @GetMapping("/catalog/capabilities")
    public Map<String, Object> capabilities(HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", "smartcbwtf");
        response.put("certificationStatus", "CERTIFIED_FOR_GENERATION");
        response.put("capabilityFamilies", List.of(
                "facility_and_master_data",
                "healthcare_facilities_and_contracts",
                "qr_labels_barcodes_and_chain_of_custody",
                "routes_vehicles_and_collections",
                "weighment_and_receiving",
                "compliance_and_reporting",
                "billing_payments_and_customer_accounts",
                "inventory_consumables_and_orders",
                "profiles_users_settings_and_audit"));
        response.put("endpoints", endpoints(request));
        response.put("triggers", List.of(
                "smartcbwtf_hcf_created",
                "smartcbwtf_contract_expiring",
                "smartcbwtf_barcode_scanned",
                "smartcbwtf_pickup_completed",
                "smartcbwtf_invoice_created",
                "smartcbwtf_payment_recorded"));
        return response;
    }

    @GetMapping("/audit-events")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN','HCF_ADMIN','TOP_MANAGEMENT')")
    public ResponseEntity<Page<AuditEventView>> auditEvents(
            @RequestParam(value = "entity_type", required = false) String entityType,
            @RequestParam(value = "entity_id", required = false) UUID entityId,
            @RequestParam(value = "actor_user_id", required = false) UUID actorUserId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        UUID actorFilter = TenantContext.isSuperAdmin() ? actorUserId : TenantContext.getUserId();
        String normalizedEntityType = normalizeEntityType(entityType);
        validateDateRange(from, to);
        Page<AuditEventView> events = auditLogRepository.search(normalizedEntityType, entityId, actorFilter, from, to,
                pageRequest(page, size, 50, 200, Sort.by(Sort.Direction.DESC, "ts")))
                .map(this::auditEventView);
        return privateResponse(events);
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private Map<String, Object> connectionMetadata(HttpServletRequest request, Facility facility, Hcf hcf) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "smartcbwtf");
        metadata.put("environment", environment);
        metadata.put("apiBaseUrl", apiBaseUrl(request));
        metadata.put("tenantId", facility != null ? facility.getId() : null);
        metadata.put("operatorId", facility != null ? facility.getId() : null);
        metadata.put("facilityId", facility != null ? facility.getId() : null);
        metadata.put("facilityDisplayName", facility != null ? facility.getName() : null);
        metadata.put("facilityCode", facility != null ? facility.getCode() : null);
        metadata.put("hcfId", hcf != null ? hcf.getId() : null);
        metadata.put("hcfName", hcf != null ? hcf.getName() : null);
        return metadata;
    }

    private static String apiBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }

    private Map<String, Object> counts(String role, TenantContext.TenantInfo tenant) {
        Map<String, Object> counts = new LinkedHashMap<>();
        if ("SUPER_ADMIN".equals(role)) {
            counts.put("facilities", facilityRepository.count());
            counts.put("hcfs", hcfRepository.count());
            counts.put("users", appUserRepository.count());
            counts.put("agreements", agreementRepository.count());
            counts.put("vehicles", vehicleRepository.count());
            counts.put("routes", routeRepository.count());
            counts.put("qrLabels", bagLabelRepository.count());
            return counts;
        }
        UUID tenantId = tenant != null ? tenant.tenantId() : null;
        UUID hcfId = tenant != null ? tenant.hcfId() : null;
        if (tenantId != null) {
            counts.put("activeHcfs", agreementRepository.countActiveByFacilityId(tenantId));
            counts.put("agreements", agreementRepository.countByFacilityId(tenantId));
            counts.put("activeVehicles", vehicleRepository.countByFacilityIdAndStatus(tenantId, "ACTIVE"));
            counts.put("activeRoutes", routeRepository.countByFacilityIdAndIsActiveTrue(tenantId));
            counts.put("qrLabels", bagLabelRepository.countByFacilityId(tenantId));
        }
        if (hcfId != null) {
            counts.put("hcfAgreements", agreementRepository.countByHcfId(hcfId));
        }
        return counts;
    }

    private List<EndpointDescriptor> descriptors(RequestMappingInfo info, HandlerMethod handlerMethod) {
        List<String> paths = info.getPatternValues().stream().filter(this::isDocumentedPath).toList();
        List<String> methods = info.getMethodsCondition().getMethods().isEmpty()
                ? List.of("ANY")
                : info.getMethodsCondition().getMethods().stream().map(Enum::name).toList();
        PreAuthorize preAuthorize = org.springframework.core.annotation.AnnotationUtils
                .findAnnotation(handlerMethod.getMethod(), PreAuthorize.class);
        if (preAuthorize == null) {
            preAuthorize = org.springframework.core.annotation.AnnotationUtils
                    .findAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
        }
        String authorization = preAuthorize == null ? UNKNOWN_AUTHORIZATION : preAuthorize.value();
        return paths.stream()
                .flatMap(path -> methods.stream().map(method -> new EndpointDescriptor(
                        method,
                        path,
                        handlerMethod.getBeanType().getSimpleName(),
                        handlerMethod.getMethod().getName(),
                        authorization,
                        "ANY".equals(method) ? null : scopeRegistry.requiredScope(method, path),
                        isMutation(method) ? "Idempotency-Key recommended" : "Cursor/date filters recommended where supported",
                        "CERTIFIED_FOR_GENERATION")))
                .toList();
    }

    private boolean isDocumentedPath(String path) {
        return path.startsWith("/api/") || path.startsWith("/oauth/") || path.startsWith("/.well-known/");
    }

    static boolean isCatalogPathVisibleForRole(String path, String role) {
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        String p = path.toLowerCase(Locale.ROOT);
        if (p.startsWith("/api/admin")
                || p.startsWith("/api/superadmin")
                || p.startsWith("/api/internal")) {
            return false;
        }
        if (isSharedCatalogPath(p)) {
            return true;
        }
        if ("CBWTF_ADMIN".equals(role)) {
            return p.startsWith("/api/cbwtf")
                    || p.startsWith("/api/facilities")
                    || p.startsWith("/api/hcfs")
                    || p.startsWith("/api/labels")
                    || p.startsWith("/api/bags")
                    || p.startsWith("/api/events")
                    || p.startsWith("/api/verify")
                    || p.startsWith("/api/location");
        }
        if ("HCF_ADMIN".equals(role)) {
            return p.startsWith("/api/hcf");
        }
        if ("TOP_MANAGEMENT".equals(role)) {
            return p.startsWith("/api/top-mgmt")
                    || p.startsWith("/api/management");
        }
        return false;
    }

    static boolean isCatalogEndpointVisibleForRole(String path, String authorization, String role) {
        if (!isCatalogPathVisibleForRole(path, role)) {
            return false;
        }
        return isAuthorizationVisibleForRole(authorization, role);
    }

    static boolean isAuthorizationVisibleForRole(String authorization, String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        if (!StringUtils.hasText(authorization)
                || UNKNOWN_AUTHORIZATION.equals(authorization)
                || authorization.contains("permitAll()")
                || authorization.contains("isAuthenticated()")) {
            return true;
        }
        Set<String> authorizedRoles = rolesFromAuthorization(authorization);
        return authorizedRoles.isEmpty() || authorizedRoles.contains(role);
    }

    private static Set<String> rolesFromAuthorization(String authorization) {
        Set<String> roles = new LinkedHashSet<>();
        if (!StringUtils.hasText(authorization)
                || (!authorization.contains("hasRole") && !authorization.contains("hasAnyRole"))) {
            return roles;
        }
        Matcher matcher = QUOTED_ROLE_PATTERN.matcher(authorization);
        while (matcher.find()) {
            roles.add(matcher.group(1));
        }
        return roles;
    }

    private static boolean isSharedCatalogPath(String p) {
        return p.startsWith("/api/integration")
                || p.startsWith("/oauth/")
                || p.equals("/.well-known/openid-configuration")
                || p.equals("/.well-known/oauth-authorization-server")
                || p.equals("/api/auth/login")
                || p.equals("/api/config/mobile")
                || p.equals("/api/errors/report")
                || p.equals("/api/errors/mobile")
                || p.equals("/api/users/me")
                || p.equals("/api/users/me/change-password");
    }

    private boolean isMutation(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private AuditEventView auditEventView(AuditLog log) {
        return new AuditEventView(log.getId(), log.getEntityType(), log.getEntityId(), log.getAction(),
                log.getActorUserId(), log.getTs(), log.getDataJson(), log.getDataHash());
    }

    private static String normalizeEntityType(String entityType) {
        if (!StringUtils.hasText(entityType)) {
            return null;
        }
        String normalized = entityType.strip();
        if (normalized.length() > MAX_ENTITY_TYPE_LENGTH) {
            throw new IllegalArgumentException("entity_type must be " + MAX_ENTITY_TYPE_LENGTH
                    + " characters or fewer");
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) {
                throw new IllegalArgumentException("entity_type contains unsupported control characters");
            }
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
    }

    public record EndpointDescriptor(
            String method,
            String path,
            String controller,
            String operation,
            String authorization,
            String requiredOAuthScope,
            String automationContract,
            String certificationStatus) {
    }

    public record AuditEventView(
            UUID id,
            String entityType,
            UUID entityId,
            String action,
            UUID actorUserId,
            Instant timestamp,
            String data,
            String dataHash) {
    }
}
