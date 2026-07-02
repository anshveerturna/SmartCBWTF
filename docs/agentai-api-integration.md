# SmartCBWTF AgentAI API Integration Guide

This guide documents the native API integration layer added for AgentAI and other agentic platforms.

The live source of truth for every callable API is now available at runtime:

- `GET /v3/api-docs` - OpenAPI JSON generated from the Spring controllers when `EXPOSE_API_DOCS=true` or `SPRINGDOC_API_DOCS_ENABLED=true`.
- `GET /swagger-ui/index.html` - interactive Swagger UI when `EXPOSE_API_DOCS=true` or `SPRINGDOC_SWAGGER_UI_ENABLED=true`.
- `GET /api/integration/catalog/endpoints` - role, controller, operation, method, path, and required OAuth scope for every API route.
- `GET /api/integration/catalog/capabilities` - provider capability families and mapped endpoints.
- `GET /api/integration/scopes` - complete scope matrix for `SUPER_ADMIN`, `CBWTF_ADMIN`, `HCF_ADMIN`, and field users.

Production keeps OpenAPI and Swagger disabled unless explicitly enabled for certification. AgentAI clients should rely on the authenticated integration catalog for production discovery.

## Authentication Modes

Existing portal JWT login remains supported:

```bash
export SMARTCBWTF_USERNAME='your-username'
export SMARTCBWTF_PASSWORD='your-password'

curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$SMARTCBWTF_USERNAME\",\"password\":\"$SMARTCBWTF_PASSWORD\"}"
```

Native AgentAI integration uses OAuth-issued bearer tokens.

OAuth endpoints:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/.well-known/oauth-authorization-server` | OAuth authorization-server metadata |
| `GET` | `/.well-known/openid-configuration` | Compatibility alias returning the same OAuth metadata |
| `GET` | `/oauth/authorize` | Authorization-code + PKCE code issue for an authenticated user |
| `POST` | `/oauth/token` | Token exchange for `authorization_code`, `refresh_token`, and `client_credentials` |
| `GET` | `/oauth/userinfo` | Connected user, tenant, HCF, role, client, and scope metadata |
| `POST` | `/oauth/introspect` | Token validation for external runtimes |
| `POST` | `/oauth/revoke` | Refresh-token revocation |
| `GET` | `/api/admin/oauth/clients` | Super admin lists OAuth clients |
| `POST` | `/api/admin/oauth/clients` | Super admin creates OAuth clients |
| `PATCH` | `/api/admin/oauth/clients/{clientId}/disable` | Super admin disables an OAuth client |

Access tokens include the same user role and tenant/HCF claims used by the portal, plus `client_id`, `scope`, `token_use`, and `grant_type`. Role checks still run through existing `@PreAuthorize` annotations. OAuth tokens add module scope checks on top. External runtimes should validate access tokens through `/oauth/introspect`; the current deployment does not advertise a JWKS because access tokens are signed with the server-side JWT secret.

## Role Scope Model

`SUPER_ADMIN` can manage platform, users, OAuth clients, audit, and all operational modules.

`CBWTF_ADMIN` can operate the facility boundary: HCFs, agreements, QR labels, collections, routes, vehicles, compliance, billing, inventory, documents, audit export, and webhooks.

`HCF_ADMIN` is limited to its own HCF context: profile, HCF read/write where allowed, contracts read, QR/manifest operations, collection reads, compliance reads, billing reads, consumable orders, and documents.

Field roles receive route, collection, manifest, weighment, and profile scopes only.

## AgentAI Request Headers

All API responses now return:

- `X-Request-Id`
- `X-Trace-Id`

AgentAI should send these on mutating calls:

- `Idempotency-Key`
- `X-AgentAI-Run-Id`
- `X-AgentAI-Workflow-Id`
- `X-AgentAI-Node-Id`
- `X-AgentAI-Reason`

Mutating requests with `Idempotency-Key` are replay-protected for JSON/text responses. A repeat with the same body returns the cached response and `X-Idempotent-Replay: true`; a repeat with a different body returns `409 IDEMPOTENCY_CONFLICT`.

Mutating AgentAI/OAuth requests are written to the audit log with request id, client id, scope, workflow/run/node ids, tenant id, HCF id, and reason.

## Connection Probe

After storing an OAuth credential, AgentAI should call:

```bash
curl -s http://localhost:8080/api/integration/probe \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

The response contains provider identity, connected user, tenant/facility/HCF binding, granted scopes, readiness flags, operational counts, and documentation URLs.

## Client Credentials Flow

Create a client as `SUPER_ADMIN`:

```bash
curl -s -X POST http://localhost:8080/api/admin/oauth/clients \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "AgentAI Sandbox",
    "serviceAccountUserId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
    "redirectUris": "http://localhost:3000/oauth/callback",
    "scopes": "smartcbwtf.profile.read smartcbwtf.facility.read smartcbwtf.hcf.read smartcbwtf.hcf.write smartcbwtf.contracts.read smartcbwtf.manifests.read smartcbwtf.manifests.write smartcbwtf.collections.read smartcbwtf.routes.read smartcbwtf.billing.read smartcbwtf.audit.read",
    "grantTypes": "client_credentials authorization_code refresh_token"
  }'
```

The `clientSecret` is returned once in the create response.

Request a machine token:

```bash
curl -s -X POST http://localhost:8080/oauth/token \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&scope=smartcbwtf.profile.read smartcbwtf.facility.read smartcbwtf.hcf.read'
```

Validate it:

```bash
curl -s -X POST http://localhost:8080/oauth/introspect \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d "token=$ACCESS_TOKEN"
```

## Authorization Code With PKCE

For user-consented connections:

1. AgentAI creates `state`, `code_verifier`, and `S256 code_challenge`.
2. The authenticated SmartCBWTF user opens `/oauth/authorize`.
3. SmartCBWTF redirects to the registered callback with `code` and `state`.
4. AgentAI backend exchanges `code` at `/oauth/token` using the original `code_verifier`.
5. AgentAI stores the returned credential against the exact tenant/facility/HCF metadata from `/api/integration/probe`.

Token exchange:

```bash
curl -s -X POST http://localhost:8080/oauth/token \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d "grant_type=authorization_code&code=$CODE&redirect_uri=$REDIRECT_URI&code_verifier=$CODE_VERIFIER"
```

## Testing Checklist

Run backend tests:

```bash
cd /Users/anshveerturna/Documents/SmartCBWTF/backend
mvn test
```

Smoke-test integration metadata:

```bash
curl -s http://localhost:8080/.well-known/oauth-authorization-server
curl -s http://localhost:8080/v3/api-docs # only when the server was started with EXPOSE_API_DOCS=true
curl -s http://localhost:8080/api/integration/catalog/endpoints -H "Authorization: Bearer $ACCESS_TOKEN"
curl -s http://localhost:8080/api/integration/catalog/capabilities -H "Authorization: Bearer $ACCESS_TOKEN"
curl -s http://localhost:8080/api/integration/probe -H "Authorization: Bearer $ACCESS_TOKEN"
```

Negative tests to run for certification:

- Use an HCF-scoped token against `/api/admin/oauth/clients`; expect `403`.
- Remove `smartcbwtf.hcf.read` and call `/api/cbwtf/hcfs`; expect `403 MISSING_SCOPE`.
- Reuse an `Idempotency-Key` with a different body; expect `409 IDEMPOTENCY_CONFLICT`.
- Revoke a refresh token with `/oauth/revoke`; refresh should fail after revocation.
- Connect two facilities and verify `/api/integration/probe` never swaps tenant or HCF binding.

## Current Certification Status

This implementation moves SmartCBWTF from an undocumented portal-only surface to a documented, OAuth-capable, role-aware native API surface.

The provider should still be marked `CERTIFIED_FOR_GENERATION` until tenant-level sandbox evidence exists for each capability slice. Promote individual slices to execution only after OAuth, scope checks, idempotency, audit receipt, negative-path tests, and cleanup are proven against the target tenant.
