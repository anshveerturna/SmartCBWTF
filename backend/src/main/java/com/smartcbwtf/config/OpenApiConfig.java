package com.smartcbwtf.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "SmartCBWTF Agentic API",
        version = "v1",
        description = "Role-aware SmartCBWTF API surface for AgentAI/native platform integrations. Use /api/integration/catalog/endpoints for the live endpoint catalog and /oauth/* for OAuth.",
        contact = @Contact(name = "SmartCBWTF", email = "info@smartcbwtf.com")))
@SecurityScheme(
        name = "SmartCBWTFOAuth",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "/oauth/authorize",
                        tokenUrl = "/oauth/token",
                        scopes = {
                                @OAuthScope(name = "offline_access", description = "Issue refresh tokens for durable workflows"),
                                @OAuthScope(name = "smartcbwtf.profile.read", description = "Read connected user profile"),
                                @OAuthScope(name = "smartcbwtf.profile.write", description = "Update connected user profile"),
                                @OAuthScope(name = "smartcbwtf.platform.read", description = "Read platform administration metadata"),
                                @OAuthScope(name = "smartcbwtf.platform.write", description = "Update platform administration settings"),
                                @OAuthScope(name = "smartcbwtf.oauth.manage", description = "Manage OAuth clients"),
                                @OAuthScope(name = "smartcbwtf.users.read", description = "Read user and staff accounts"),
                                @OAuthScope(name = "smartcbwtf.users.write", description = "Create, update, disable, or unlock user and staff accounts"),
                                @OAuthScope(name = "smartcbwtf.facility.read", description = "Read facility metadata and settings"),
                                @OAuthScope(name = "smartcbwtf.facility.write", description = "Update facility metadata and settings"),
                                @OAuthScope(name = "smartcbwtf.hcf.read", description = "Read healthcare facilities"),
                                @OAuthScope(name = "smartcbwtf.hcf.write", description = "Create or update healthcare facilities"),
                                @OAuthScope(name = "smartcbwtf.contracts.read", description = "Read agreements and terms"),
                                @OAuthScope(name = "smartcbwtf.contracts.write", description = "Create, renew, or update agreements"),
                                @OAuthScope(name = "smartcbwtf.manifests.read", description = "Read QR labels, bags, and chain-of-custody data"),
                                @OAuthScope(name = "smartcbwtf.manifests.write", description = "Generate QR labels and record scan events"),
                                @OAuthScope(name = "smartcbwtf.collections.read", description = "Read pickup, attendance, location, and waste collection data"),
                                @OAuthScope(name = "smartcbwtf.collections.write", description = "Record pickup, attendance, GPS, and collection events"),
                                @OAuthScope(name = "smartcbwtf.routes.read", description = "Read routes and waypoints"),
                                @OAuthScope(name = "smartcbwtf.routes.write", description = "Create or update routes and assignments"),
                                @OAuthScope(name = "smartcbwtf.vehicles.read", description = "Read vehicle and GPS state"),
                                @OAuthScope(name = "smartcbwtf.vehicles.write", description = "Update vehicles and GPS integrations"),
                                @OAuthScope(name = "smartcbwtf.weighments.read", description = "Read weighment data"),
                                @OAuthScope(name = "smartcbwtf.weighments.write", description = "Record weighment data"),
                                @OAuthScope(name = "smartcbwtf.treatment.read", description = "Read treatment operations"),
                                @OAuthScope(name = "smartcbwtf.treatment.write", description = "Record treatment operations"),
                                @OAuthScope(name = "smartcbwtf.disposal.read", description = "Read disposal operations"),
                                @OAuthScope(name = "smartcbwtf.disposal.write", description = "Record disposal operations"),
                                @OAuthScope(name = "smartcbwtf.compliance.read", description = "Read compliance reports and obligations"),
                                @OAuthScope(name = "smartcbwtf.compliance.write", description = "Generate or submit compliance artifacts"),
                                @OAuthScope(name = "smartcbwtf.billing.read", description = "Read bills, invoices, and payments"),
                                @OAuthScope(name = "smartcbwtf.billing.write", description = "Generate bills, invoices, and payment receipts"),
                                @OAuthScope(name = "smartcbwtf.inventory.read", description = "Read consumables and inventory data"),
                                @OAuthScope(name = "smartcbwtf.inventory.write", description = "Create or update consumables and orders"),
                                @OAuthScope(name = "smartcbwtf.maintenance.read", description = "Read maintenance records"),
                                @OAuthScope(name = "smartcbwtf.maintenance.write", description = "Create or update maintenance records"),
                                @OAuthScope(name = "smartcbwtf.incidents.read", description = "Read incident and error reports"),
                                @OAuthScope(name = "smartcbwtf.incidents.write", description = "Create or update incident and error reports"),
                                @OAuthScope(name = "smartcbwtf.documents.read", description = "Read generated documents"),
                                @OAuthScope(name = "smartcbwtf.documents.write", description = "Generate or update documents"),
                                @OAuthScope(name = "smartcbwtf.audit.read", description = "Read audit events"),
                                @OAuthScope(name = "smartcbwtf.webhooks.manage", description = "Manage webhook and outbound integration settings")
                        })))
public class OpenApiConfig {
}
