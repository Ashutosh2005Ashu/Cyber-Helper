# Cyber-Helper: Week 2 - RBAC & User Management API Documentation

## Overview
Week 2 implementation adds comprehensive Role-Based Access Control (RBAC) and user/organization management features to the Cyber-Helper backend.

## Key Concepts

### Organization Aliases (Multi-Tenancy)
The system supports multiple organization names referring to the same legal entity:
- **Master Organization**: The canonical organization (e.g., "BSE")
- **Aliases**: Variations of the organization name (e.g., "BSE pvt", "BSE pvt ltd")
- All aliases resolve to the same master organization for data queries
- Users from any alias see identical compliance data

### Role-Based Access Control
- **ADMIN**: Full management access (can CRUD users, organizations, roles)
- **RE_USER**: Regulated Entity user (can view compliance data)
- **AUDITOR**: Can audit compliance (can generate reports)
- **SEBI_OFFICER**: SEBI staff (can review/approve compliance)

## API Endpoints

### Authentication (Week 1 - Unchanged)
All endpoints require JWT Bearer token in Authorization header.

```
POST /api/auth/register
  Request: {
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "organizationId": 1,
    "roleIds": [1, 2]
  }
  Response: {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": { ... }
  }

POST /api/auth/login
  Request: {
    "email": "user@example.com",
    "password": "password123"
  }
  Response: { ... }
```

### Organization Management (Week 2 - NEW)

#### Create Organization
```
POST /api/organizations
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

Request:
{
  "orgName": "BSE",
  "orgType": "STOCK_EXCHANGE",
  "registrationNo": "SEBI-001",
  "masterOrgId": null
}

Response: (201 Created)
{
  "id": 1,
  "orgName": "BSE",
  "orgType": "STOCK_EXCHANGE",
  "registrationNo": "SEBI-001",
  "status": "ACTIVE",
  "isMaster": true,
  "masterOrgId": null,
  "createdAt": "2026-06-26T12:00:00Z",
  "updatedAt": "2026-06-26T12:00:00Z"
}
```

#### Create Organization Alias
```
POST /api/organizations
Authorization: Bearer <ADMIN_TOKEN>

Request:
{
  "orgName": "BSE PVT",
  "orgType": "STOCK_EXCHANGE",
  "registrationNo": "SEBI-001",
  "masterOrgId": 1  # Points to master BSE organization
}

Response: (201 Created)
{
  "id": 2,
  "orgName": "BSE PVT",
  "orgType": "STOCK_EXCHANGE",
  "registrationNo": "SEBI-001",
  "status": "ACTIVE",
  "isMaster": false,
  "masterOrgId": 1,
  "createdAt": "2026-06-26T12:01:00Z",
  "updatedAt": "2026-06-26T12:01:00Z"
}
```

#### List All Organizations
```
GET /api/organizations
Authorization: Bearer <TOKEN>

Response: (200 OK)
[
  { ... master organizations ... },
  { ... alias organizations ... }
]
```

#### Get Master Organizations Only
```
GET /api/organizations/master
Authorization: Bearer <TOKEN>

Response: (200 OK)
[
  { "id": 1, "orgName": "BSE", "isMaster": true, ... },
  { "id": 3, "orgName": "NSE", "isMaster": true, ... }
]
```

#### Get Organization Aliases
```
GET /api/organizations/{masterId}/aliases
Authorization: Bearer <TOKEN>

Response: (200 OK)
[
  { "id": 2, "orgName": "BSE PVT", "isMaster": false, "masterOrgId": 1, ... },
  { "id": 4, "orgName": "BSE PVT LTD", "isMaster": false, "masterOrgId": 1, ... }
]
```

#### Get Organization by ID
```
GET /api/organizations/{id}
Authorization: Bearer <TOKEN>

Response: (200 OK)
{ ... organization details ... }
```

#### Update Organization
```
PUT /api/organizations/{id}
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

Request:
{
  "orgName": "BSE Updated",
  "orgType": "STOCK_EXCHANGE",
  "registrationNo": "SEBI-001-NEW",
  "masterOrgId": null
}

Response: (200 OK)
{ ... updated organization ... }
```

#### Delete Organization
```
DELETE /api/organizations/{id}
Authorization: Bearer <ADMIN_TOKEN>

Response: (204 No Content)
```

#### Change Organization Status
```
PATCH /api/organizations/{id}/status?status=INACTIVE
Authorization: Bearer <ADMIN_TOKEN>

Response: (200 OK)
{ ... organization with new status ... }
```

### User Management (Week 2 - ENHANCED)

#### Create User (via Registration)
```
POST /api/auth/register
See Authentication section above
```

#### Get Current User
```
GET /api/users/me
Authorization: Bearer <USER_TOKEN>

Response: (200 OK)
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "organizationName": "BSE",
  "roles": ["RE_USER", "AUDITOR"]
}
```

#### Get User by ID
```
GET /api/users/{id}
Authorization: Bearer <TOKEN>

Response: (200 OK)
{ ... user details ... }

Note: Non-ADMIN users can only view their own profile
```

#### Get All Users (ADMIN only)
```
GET /api/users
Authorization: Bearer <ADMIN_TOKEN>

Response: (200 OK)
[
  { "id": 1, "email": "user1@example.com", ... },
  { "id": 2, "email": "user2@example.com", ... }
]
```

#### Get Users by Organization (ADMIN only)
```
GET /api/users/organization/{orgId}
Authorization: Bearer <ADMIN_TOKEN>

Response: (200 OK)
[ ... users belonging to organization {orgId} ... ]
```

#### Search Users by Email (ADMIN only)
```
GET /api/users/search?email=john
Authorization: Bearer <ADMIN_TOKEN>

Response: (200 OK)
[
  { "id": 1, "email": "john@example.com", ... },
  { "id": 3, "email": "john.doe@example.com", ... }
]
```

#### Update User
```
PUT /api/users/{id}
Authorization: Bearer <TOKEN>
Content-Type: application/json

Request:
{
  "email": "newemail@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "organizationId": 2,
  "roleIds": [1, 3]
}

Response: (200 OK)
{ ... updated user ... }

Note: Non-ADMIN users can only update their own profile
```

#### Delete User (ADMIN only)
```
DELETE /api/users/{id}
Authorization: Bearer <ADMIN_TOKEN>

Response: (204 No Content)
```

#### Assign Roles to User (ADMIN only)
```
POST /api/users/{id}/roles
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

Request:
{
  "roleIds": [1, 2, 3]
}

Response: (200 OK)
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "organizationName": "BSE",
  "roles": ["RE_USER", "AUDITOR", "ADMIN"]
}
```

## Authorization Rules

### Organization Endpoints
| Operation | ADMIN | RE_USER | AUDITOR | SEBI_OFFICER |
|-----------|-------|---------|---------|--------------|
| GET (view) | ✓ | ✓ | ✓ | ✓ |
| POST (create) | ✓ | ✗ | ✗ | ✗ |
| PUT (update) | ✓ | ✗ | ✗ | ✗ |
| DELETE | ✓ | ✗ | ✗ | ✗ |
| PATCH (status) | ✓ | ✗ | ✗ | ✗ |

### User Endpoints
| Operation | ADMIN | Self | Others |
|-----------|-------|------|--------|
| GET /me | ✓ | ✓ | - |
| GET /{id} | ✓ | ✓ | ✗ |
| GET (list all) | ✓ | ✗ | - |
| GET /organization/{id} | ✓ | ✗ | - |
| GET /search | ✓ | ✗ | - |
| PUT /{id} | ✓ | ✓ (own) | ✗ |
| DELETE | ✓ | ✗ | - |
| POST /{id}/roles | ✓ | ✗ | - |

## Error Responses

### 400 Bad Request
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request body",
  "path": "/api/organizations"
}
```

### 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/users"
}
```

### 403 Forbidden
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Only ADMIN users can access this resource",
  "path": "/api/organizations"
}
```

### 404 Not Found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Organization not found: 999",
  "path": "/api/organizations/999"
}
```

### 409 Conflict
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Organization with name 'BSE' already exists",
  "path": "/api/organizations"
}
```

## Testing

### Manual Testing with cURL

#### 1. Register Admin User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@test.com",
    "password": "password123",
    "firstName": "Admin",
    "lastName": "User",
    "organizationId": 1,
    "roleIds": [1]
  }'
```

#### 2. Create Organization
```bash
curl -X POST http://localhost:8080/api/organizations \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "orgName": "BSE",
    "orgType": "STOCK_EXCHANGE",
    "registrationNo": "SEBI-001",
    "masterOrgId": null
  }'
```

#### 3. Get All Organizations
```bash
curl -X GET http://localhost:8080/api/organizations \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

#### 4. Create Organization Alias
```bash
curl -X POST http://localhost:8080/api/organizations \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "orgName": "BSE PVT",
    "orgType": "STOCK_EXCHANGE",
    "registrationNo": "SEBI-001",
    "masterOrgId": 1
  }'
```

## Integration with Week 1

All Week 1 features remain unchanged:
- ✓ PostgreSQL integration
- ✓ User registration & login
- ✓ JWT authentication
- ✓ Spring Security configuration
- ✓ Entity relationships

Week 2 adds:
- ✓ Enhanced user management (CRUD)
- ✓ Organization management (CRUD)
- ✓ Authorization layer (@PreAuthorize)
- ✓ Role assignment operations
- ✓ Multi-tenancy via organization aliases
- ✓ Organization membership isolation

## Next Steps (Week 3+)
- Document upload and management
- PDF processing and text extraction
- Vector database integration
- Embedding generation
- RAG implementation
- Compliance features
