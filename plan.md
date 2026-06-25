# Cyber-Helper: SEBI Cybersecurity Compliance RAG Assistant
## 8-Week Development Plan

---

## PROJECT OVERVIEW

**Goal:** Build an enterprise-grade RAG chatbot that answers SEBI cybersecurity compliance questions using retrieved documents (never relying on LLM internal knowledge).

**Tech Stack:** Java 21, Spring Boot, PostgreSQL, Qdrant, Ollama (Llama3), nomic-embed-text, Docker Compose

**Architecture:** Clean layered architecture (Controller → Service → Repository → Database)

---

## 8-WEEK ROADMAP (HIGH LEVEL)

### Week 1: Authentication Foundation ✓ CURRENT
- PostgreSQL + Spring Boot integration
- Role entity (ADMIN, RE_USER, AUDITOR, SEBI_OFFICER)
- Organization entity
- User entity with relationships
- JWT auth (simple access tokens)
- Spring Security configuration
- Auth endpoints (register, login)
- Table creation verification via Hibernate

### Week 2: RBAC & User Management
- User role assignment
- Organization membership
- User management endpoints (CRUD)
- Organization management endpoints
- Authorization filters/annotations
- Role-based endpoint access

### Week 3: Document Upload
- Document entity
- File storage strategy (local FS or cloud)
- Document upload endpoint
- Document listing/retrieval
- File type validation (PDF only initially)

### Week 4: PDF Processing
- Apache PDFBox integration
- PDF text extraction
- Document processing service
- Text validation & cleanup
- Raw text storage in DB

### Week 5: Vector Database
- Qdrant integration (Docker Compose)
- Document chunking strategy
- Embedding metadata model
- Chunk storage in Qdrant
- Retrieval service foundation

### Week 6: Embedding Generation
- Ollama + nomic-embed-text setup
- Embedding generation pipeline
- Batch embedding processing
- Vector storage coordination

### Week 7: RAG Implementation
- Llama3 integration
- Query embedding generation
- Semantic search against Qdrant
- Prompt engineering (context + question)
- Answer generation with citations

### Week 8: Compliance Features
- Audit checklist generation
- Gap analysis service
- Role-based response filtering
- Compliance report generation

---

## WEEK 1 DETAILED BREAKDOWN

### Objective
Create a minimal but production-quality authentication foundation with PostgreSQL, JWT, and core entities.

### Architecture

```
┌─────────────┐
│ Controller  │ (AuthController, UserController)
├─────────────┤
│  Service    │ (AuthService, UserService)
├─────────────┤
│ Repository  │ (UserRepository, RoleRepository, OrganizationRepository)
├─────────────┤
│   Entity    │ (User, Role, Organization)
├─────────────┤
│  PostgreSQL │
└─────────────┘

Cross-cutting:
- Spring Security configuration
- JWT TokenProvider
- GlobalExceptionHandler
- DTOs (requests/responses)
```

### Entities to Create

#### 1. Role Entity
- **Purpose:** Enum-like entity for user roles (ADMIN, RE_USER, AUDITOR, SEBI_OFFICER)
- **Fields:** id (PK), roleName (UNIQUE), description, createdAt
- **Why:** Allows dynamic role management; joins users via many-to-many relationship

#### 2. Organization Entity (Master + Aliases)
- **Purpose:** Represents SEBI-regulated entities with support for multiple name variations (e.g., BSE, BSE pvt, BSE pvt ltd)
- **Fields:**
    - id (PK)
    - orgName (e.g., "BSE" for master, "BSE pvt" for alias) — UNIQUE across all orgs
    - orgType (enum: STOCK_EXCHANGE, BROKER, DEPOSITORY, etc.)
    - registrationNo (SEBI registration number)
    - isMaster (boolean, default: true) — marks if this is the canonical organization
    - masterOrgId (FK, nullable) — if not master, points to canonical org; if master, null
    - status (ACTIVE, INACTIVE)
    - createdAt, updatedAt
- **Why:**
    - Admin creates "BSE" as master org (isMaster=true, masterOrgId=null)
    - Admin creates "BSE pvt" as alias (isMaster=false, masterOrgId=<BSE_id>)
    - Users can belong to any variation; queries follow masterOrgId to get canonical org
    - Compliance answers retrieved by masterOrgId ensure consistency across all aliases
    - Multi-tenancy maintained; users see only their org's data

#### 3. User Entity
- **Purpose:** System users across all roles
- **Fields:** id (PK), email (UNIQUE), passwordHash, firstName, lastName, isActive, createdAt, updatedAt
- **Relationships:**
    - Many-to-One with Organization (user belongs to one org)
    - Many-to-Many with Role (user can have multiple roles)

### Organization Alias Handling (Critical for Multi-Tenancy)

**Concept:** Multiple organizations can represent the same legal entity.

Example:
- Admin creates "BSE" as master org (isMaster=true, masterOrgId=null)
- Admin creates "BSE pvt" as alias (isMaster=false, masterOrgId=BSE.id)
- Admin creates "BSE pvt ltd" as alias (isMaster=false, masterOrgId=BSE.id)
- All three are separate org records but refer to same entity

**Query Strategy:**

When loading org-scoped data (documents, checklists, compliance rules):

```java
// In OrganizationRepository:
public Organization getCanonicalOrganization(Long orgId) {
    Organization org = findById(orgId);
    if (org.isMaster()) {
        return org;
    } else {
        return findById(org.masterOrgId); // resolve alias
    }
}
```

**User Experience:**
- User "john@bsepvt" belongs to "BSE pvt" org
- User "jane@bse" belongs to "BSE" (master) org
- User "bob@bsepvtltd" belongs to "BSE pvt ltd" org
- All three receive identical compliance documents when they query → because all belong to same canonical org (BSE)

### DTOs (Simple)

#### AuthRequest
- email (required, email format)
- password (required, min 8 chars)

#### AuthResponse
- accessToken
- tokenType ("Bearer")
- expiresIn (seconds)
- user (UserDTO)

#### UserDTO
- id, email, firstName, lastName, organizationName, roles[]

#### UserRegisterRequest
- email, password, firstName, lastName, organizationId, roleIds[]

### Services

#### AuthService
- authenticate(email, password): AuthResponse
- register(UserRegisterRequest): UserDTO
- Delegates actual business logic; validates JWT token format

#### UserService
- getUserById(id): UserDTO
- getUserByEmail(email): UserDTO
- Service layer only; repository-level queries abstracted

#### RoleService
- getAllRoles(): List<RoleDTO>
- Bootstraps initial roles at startup

### Controllers

#### AuthController
- POST /api/auth/register → AuthResponse
- POST /api/auth/login → AuthResponse
- Handles auth requests; returns JWT

#### UserController
- GET /api/users/{id} → UserDTO (authorization check)
- GET /api/users/me → UserDTO (current user)
- Restricted endpoints; requires valid JWT

### Security Components

#### JwtTokenProvider
- generateToken(userId, roles): String
- validateToken(token): Boolean
- extractUserId(token): Long
- extractRoles(token): List<String>
- Token expiry: 1 hour (configurable)

#### SecurityConfig
- Disable CSRF (stateless)
- Enable CORS (placeholder for React)
- Filter chain: JWT filter before authentication
- Password encoder: BCrypt

#### JwtAuthenticationFilter
- Intercepts requests
- Extracts JWT from Authorization header
- Validates & populates SecurityContext
- Returns 401 if token invalid

### Exception Handling

#### GlobalExceptionHandler
- UserNotFoundException → 404
- InvalidCredentialsException → 401
- InvalidTokenException → 401
- ValidationException → 400
- Generic → 500 (log & return safe message)

### Task Breakdown

1. **setup-db-config** - Configure PostgreSQL in application.yml, add dependencies
2. **create-role-entity** - Role entity with enum conversion
3. **create-organization-entity** - Organization entity with validation
4. **create-user-entity** - User entity with relationships, password handling
5. **create-repositories** - UserRepository, RoleRepository, OrganizationRepository with queries
6. **verify-hibernate** - Run Spring Boot, verify table creation
7. **create-security-config** - Spring Security, BCrypt, JWT setup
8. **create-jwt-provider** - Token generation/validation logic
9. **create-jwt-filter** - Authentication filter
10. **create-auth-service** - Business logic for auth
11. **create-user-service** - Business logic for user management
12. **create-auth-controller** - Registration & login endpoints
13. **create-user-controller** - User retrieval endpoints
14. **create-exception-handler** - Centralized error responses
15. **create-dtos** - Request/response objects
16. **setup-role-bootstrap** - Initialize roles at startup
17. **integration-test-auth** - Test register, login, protected endpoints
18. **documentation-readme** - API docs for Week 1

### Dependencies to Add
```xml
<!-- Spring Security & JWT -->
<groupId>io.jsonwebtoken</groupId>
<artifactId>jjwt-api</artifactId>
<version>0.12.3</version>

<groupId>io.jsonwebtoken</groupId>
<artifactId>jjwt-impl</artifactId>
<version>0.12.3</version>
<scope>runtime</scope>

<groupId>io.jsonwebtoken</groupId>
<artifactId>jjwt-jackson</artifactId>
<version>0.12.3</version>
<scope>runtime</scope>

<!-- Lombok (convenience) -->
<groupId>org.projectlombok</groupId>
<artifactId>lombok</artifactId>
<optional>true</optional>
```

### Important Decisions

1. **DTO Separation:** Never expose entities directly; all responses use DTOs.
2. **Password Handling:** Never store plain passwords; use BCrypt in UserService.register().
3. **JWT Expiry:** 1 hour for access token (short-lived for security).
4. **Role Initialization:** Bootstrap roles in a CommandLineRunner; idempotent (check before insert).
5. **Exception Handling:** All endpoints return consistent JSON error responses.
6. **Constructor Injection:** All services use constructor injection (Lombok @RequiredArgsConstructor).

### Success Criteria
- ✓ PostgreSQL tables created (verified via logs)
- ✓ User registration endpoint works (returns JWT)
- ✓ User login endpoint works (validates credentials)
- ✓ Protected endpoints reject invalid tokens
- ✓ Role assignment works (ADMIN, RE_USER, AUDITOR, SEBI_OFFICER)
- ✓ JWT tokens decode correctly
- ✓ Integration tests pass

---

## WEEKS 2-8: IMPLEMENTATION NOTES

### Week 2: RBAC (Notes)
- Add @PreAuthorize annotations to controllers
- Create authorization aspects
- Organization filtering in queries (users see only their org's data)
- No new entities; reuse from Week 1

### Week 3: Document Upload (Notes)
- Document entity: name, orgId, uploadedBy, status, filePath, createdAt
- Handle multipart/form-data requests
- Validate file size & extension
- Store files in `storage/documents/{orgId}/{documentId}/`

### Week 4: PDF Processing (Notes)
- DocumentProcessingService: extract text using PDFBox
- Store raw extracted text in Document.rawText
- Queue for chunking (Week 5)

### Week 5: Vector Database (Notes)
- Qdrant Docker service in docker-compose
- Chunking strategy: 256-token overlapping chunks (32 token overlap)
- DocumentChunk entity: documentId, chunkIndex, text, embeddingId
- RetrievalService interface (implementation in Week 6-7)

### Week 6: Ollama Integration (Notes)
- Ollama container in docker-compose
- EmbeddingService: calls Ollama for embeddings
- Batch embedding for document chunks
- Store embedding vectors in Qdrant

### Week 7: RAG Implementation (Notes)
- RAGService: orchestrates retrieval + generation
- Prompt template: "{context}\n\nQuestion: {question}\n\nAnswer:"
- Cite sources from retrieved chunks
- Stream response to user (optional; batch response acceptable)

### Week 8: Compliance Features (Notes)
- ComplianceChecklistService: generates role-specific checklists
- GapAnalysisService: compares user org config vs. regulations
- ReportGenerationService: creates PDF compliance reports

---

## ARCHITECTURE PRINCIPLES (ENFORCED)

✓ **Controllers never access repositories directly**
✓ **Business logic lives in services only**
✓ **Repositories interact with DB only**
✓ **All external APIs return DTOs**
✓ **All inputs validated at controller layer**
✓ **Constructor injection everywhere**
✓ **No placeholder code; production-quality**
✓ **Layered architecture strictly maintained**

---

## DEPLOYMENT NOTES

- Docker Compose includes: PostgreSQL (now), Qdrant (Week 5), Ollama (Week 6)
- Each container has health checks
- Volumes for persistence (DB data, document storage, Ollama models)
- Network: shared bridge network for service communication
- Environment variables: externalized in .env

---

## SUMMARY

Week 1 focuses on a solid, minimal authentication foundation. No shortcuts. Every class justified. Every relationship necessary. Production-ready code with proper error handling, logging, and separation of concerns.

After Week 1: All future features (documents, RAG, compliance) build cleanly on this foundation.
