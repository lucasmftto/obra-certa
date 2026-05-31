# Obra Certa — Functional Spec: Projects Module

## Overview

The Projects module is the system's entry point. Everything else (environments, items, expenses) exists within a project. The user creates a project for each renovation they want to budget and track.

---

## Status lifecycle

```
IN_BUDGET → IN_PROGRESS → COMPLETED
                ↓
            ON_HOLD → IN_PROGRESS
```

| Status | Description |
|---|---|
| `IN_BUDGET` | Project created; budget is still being assembled. No expenses allowed. |
| `IN_PROGRESS` | Work has started. Expenses can be recorded. |
| `ON_HOLD` | Work temporarily paused. Expenses cannot be recorded. |
| `COMPLETED` | Work finished. No changes allowed. |

---

## Business rules

### BR-PROJ-01 — Creation
- Every project must have at least a name and a type (`HOUSE` or `APARTMENT`).
- Initial status is always `IN_BUDGET`.
- `createdAt` is set automatically by the system.

### BR-PROJ-02 — Editing
- Name, address, type, and description can be edited in any status except `COMPLETED`.
- `COMPLETED` status locks the project and all its children (environments, items, expenses).

### BR-PROJ-03 — Status transitions
- Only transitions defined in the diagram above are allowed.
- To move from `IN_BUDGET` to `IN_PROGRESS`, the project must have at least one environment with at least one item.
- Rolling back from `COMPLETED` to any other status is not allowed.

### BR-PROJ-04 — Deletion
- A project can only be deleted when its status is `IN_BUDGET`.
- Deletion is logical (soft delete via `deletedAt` field).
- Deleting a project cascades to all its environments, items, and expenses.

### BR-PROJ-05 — Financial calculations
The system must calculate and expose in real time:
- **Total budgeted:** sum of `(quantity × unitPrice)` across all items in the project.
- **Total spent:** sum of all expenses recorded against the project's items.
- **Remaining balance:** total budgeted − total spent.
- **Spent percentage:** (total spent / total budgeted) × 100.
- **Completion percentage:** average of `completionPercentage` across all environments.

### BR-PROJ-06 — Over-budget alert
- If `total spent > total budgeted`, the project enters a visual alert state.
- The system does not block recording expenses above the budget; it only signals the situation.

---

## Main flows

### Flow 1 — Create project
1. User opens the project list.
2. Clicks "New project".
3. Fills in name, type, address (optional), and description (optional).
4. Confirms. System creates the project with status `IN_BUDGET`.
5. User is redirected to the project detail screen.

### Flow 2 — Start work
1. User opens a project with status `IN_BUDGET`.
2. Clicks "Start work".
3. System validates that at least one environment with one item exists.
4. Status changes to `IN_PROGRESS`.

### Flow 3 — Complete work
1. User opens a project with status `IN_PROGRESS` or `ON_HOLD`.
2. Clicks "Complete work".
3. System shows a confirmation dialog with a financial summary (budgeted, spent, balance).
4. User confirms. Status changes to `COMPLETED`.

### Flow 4 — Delete project
1. User opens a project with status `IN_BUDGET`.
2. Clicks "Delete".
3. System shows a warning that all data will be removed.
4. User confirms. Soft delete is applied.

---

## Validations

| Field | Rule |
|---|---|
| `name` | Required. Min 3 chars. Max 200 chars. |
| `type` | Required. Allowed values: `HOUSE`, `APARTMENT`. |
| `address` | Optional. Max 300 chars. |
| `description` | Optional. Max 500 chars. |
| `status` | Managed by the system. Not sent by the client on creation. |

---

# Obra Certa — API Spec: Projects Module

## Base URL

```
/api/v1/projects
```

---

## Endpoints

### 1. List projects

```
GET /api/v1/projects
```

**Query params:**

| Param | Type | Required | Description |
|---|---|---|---|
| `status` | string | No | Filter by status: `IN_BUDGET`, `IN_PROGRESS`, `ON_HOLD`, `COMPLETED` |
| `type` | string | No | Filter by type: `HOUSE`, `APARTMENT` |
| `search` | string | No | Search by name or address (case-insensitive, contains) |
| `page` | int | No | Page number (default: 0) |
| `size` | int | No | Items per page (default: 20, max: 50) |
| `sort` | string | No | Sort field (default: `createdAt,desc`) |

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Moema apartment renovation",
      "type": "APARTMENT",
      "address": "Rua das Flores, 123 — Moema, SP",
      "status": "IN_PROGRESS",
      "totalBudgeted": 85000.00,
      "totalSpent": 32400.00,
      "remainingBalance": 52600.00,
      "spentPercentage": 38.12,
      "completionPercentage": 35.00,
      "overBudget": false,
      "totalEnvironments": 5,
      "createdAt": "2024-03-01T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

### 2. Get project by ID

```
GET /api/v1/projects/{id}
```

**Response 200:**
```json
{
  "id": 1,
  "name": "Moema apartment renovation",
  "type": "APARTMENT",
  "address": "Rua das Flores, 123 — Moema, SP",
  "description": "Full renovation including kitchen and bathrooms.",
  "status": "IN_PROGRESS",
  "totalBudgeted": 85000.00,
  "totalSpent": 32400.00,
  "remainingBalance": 52600.00,
  "spentPercentage": 38.12,
  "completionPercentage": 35.00,
  "overBudget": false,
  "totalEnvironments": 5,
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-15T14:30:00"
}
```

**Response 404:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Project with id 99 not found.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 3. Create project

```
POST /api/v1/projects
```

**Request body:**
```json
{
  "name": "Moema apartment renovation",
  "type": "APARTMENT",
  "address": "Rua das Flores, 123 — Moema, SP",
  "description": "Full renovation including kitchen and bathrooms."
}
```

**Response 201:**
```json
{
  "id": 1,
  "name": "Moema apartment renovation",
  "type": "APARTMENT",
  "address": "Rua das Flores, 123 — Moema, SP",
  "description": "Full renovation including kitchen and bathrooms.",
  "status": "IN_BUDGET",
  "totalBudgeted": 0.00,
  "totalSpent": 0.00,
  "remainingBalance": 0.00,
  "spentPercentage": 0.00,
  "completionPercentage": 0.00,
  "overBudget": false,
  "totalEnvironments": 0,
  "createdAt": "2024-03-20T10:00:00",
  "updatedAt": "2024-03-20T10:00:00"
}
```

**Response 422:**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "fields": [
    { "field": "name", "message": "Name is required" },
    { "field": "type", "message": "Type must be HOUSE or APARTMENT" }
  ],
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 4. Update project

```
PUT /api/v1/projects/{id}
```

**Request body:** (same schema as POST)

**Response 200:** (same schema as GET by ID)

**Response 409:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Projects with status COMPLETED cannot be edited.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 5. Update status

```
PATCH /api/v1/projects/{id}/status
```

**Request body:**
```json
{ "status": "IN_PROGRESS" }
```

**Response 200:** (same schema as GET by ID)

**Response 409 — invalid transition:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Cannot transition from IN_BUDGET to COMPLETED directly.",
  "timestamp": "2024-03-20T10:00:00"
}
```

**Response 422 — precondition not met:**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "The project must have at least one environment with one item to start work.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 6. Delete project

```
DELETE /api/v1/projects/{id}
```

**Response 204:** (no body)

**Response 409:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Only projects with status IN_BUDGET can be deleted.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 7. Project summary (feeds the dashboard)

```
GET /api/v1/projects/{id}/summary
```

**Response 200:**
```json
{
  "projectId": 1,
  "name": "Moema apartment renovation",
  "status": "IN_PROGRESS",
  "totals": {
    "budgeted": 85000.00,
    "spent": 32400.00,
    "balance": 52600.00,
    "spentPercentage": 38.12,
    "completionPercentage": 35.00,
    "overBudget": false
  },
  "byEnvironment": [
    {
      "environmentId": 1,
      "name": "Kitchen",
      "budgeted": 22000.00,
      "spent": 18500.00,
      "balance": 3500.00,
      "spentPercentage": 84.09,
      "completionPercentage": 80.00,
      "overBudget": false
    },
    {
      "environmentId": 2,
      "name": "Bathroom",
      "budgeted": 15000.00,
      "spent": 15900.00,
      "balance": -900.00,
      "spentPercentage": 106.00,
      "completionPercentage": 90.00,
      "overBudget": true
    }
  ],
  "recentExpenses": [
    {
      "expenseId": 45,
      "description": "Porcelain tile 60x60",
      "amount": 3200.00,
      "date": "2024-03-18",
      "environmentName": "Kitchen"
    }
  ]
}
```

---

## Endpoint summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/projects` | List projects (paginated) |
| `GET` | `/api/v1/projects/{id}` | Get project by ID |
| `POST` | `/api/v1/projects` | Create project |
| `PUT` | `/api/v1/projects/{id}` | Update project |
| `PATCH` | `/api/v1/projects/{id}/status` | Update status |
| `DELETE` | `/api/v1/projects/{id}` | Delete project (soft delete) |
| `GET` | `/api/v1/projects/{id}/summary` | Financial and progress summary |

---

## HTTP status codes

| Code | Usage |
|---|---|
| `200` | Successful GET, PUT, PATCH |
| `201` | Successful creation (POST) |
| `204` | Successful deletion (DELETE) |
| `404` | Resource not found |
| `409` | Business rule conflict or invalid transition |
| `422` | Field validation or precondition not met |
| `500` | Unexpected internal error |

---

# Obra Certa — Entities & Database Spec: Projects Module

## Table: `project`

| Column | PostgreSQL Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Primary key |
| `name` | `VARCHAR(200)` | NOT NULL | — | Project name |
| `type` | `VARCHAR(20)` | NOT NULL | — | `HOUSE` or `APARTMENT` |
| `address` | `VARCHAR(300)` | NULL | — | Property address |
| `description` | `VARCHAR(500)` | NULL | — | Free text description |
| `status` | `VARCHAR(20)` | NOT NULL | `'IN_BUDGET'` | Current status |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Last update timestamp |
| `deleted_at` | `TIMESTAMP` | NULL | — | Soft delete timestamp |

### Constraints

```sql
ALTER TABLE project
  ADD CONSTRAINT chk_project_type
    CHECK (type IN ('HOUSE', 'APARTMENT'));

ALTER TABLE project
  ADD CONSTRAINT chk_project_status
    CHECK (status IN ('IN_BUDGET', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED'));
```

### Indexes

```sql
CREATE INDEX idx_project_status     ON project(status)     WHERE deleted_at IS NULL;
CREATE INDEX idx_project_type       ON project(type)       WHERE deleted_at IS NULL;
CREATE INDEX idx_project_created_at ON project(created_at DESC);
```

### Flyway migration

```sql
-- V1__create_project.sql

CREATE TABLE project (
  id          BIGSERIAL    PRIMARY KEY,
  name        VARCHAR(200) NOT NULL,
  type        VARCHAR(20)  NOT NULL,
  address     VARCHAR(300),
  description VARCHAR(500),
  status      VARCHAR(20)  NOT NULL DEFAULT 'IN_BUDGET',
  created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  deleted_at  TIMESTAMP,

  CONSTRAINT chk_project_type   CHECK (type   IN ('HOUSE', 'APARTMENT')),
  CONSTRAINT chk_project_status CHECK (status IN ('IN_BUDGET', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED'))
);

CREATE INDEX idx_project_status     ON project(status)     WHERE deleted_at IS NULL;
CREATE INDEX idx_project_type       ON project(type)       WHERE deleted_at IS NULL;
CREATE INDEX idx_project_created_at ON project(created_at DESC);
```

---

## JPA Entity: `Project.java`

```java
package com.obracerta.project.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectType type;

    @Column(length = 300)
    private String address;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.IN_BUDGET;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Environment> environments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted()   { return deletedAt != null; }
    public boolean isCompleted() { return status == ProjectStatus.COMPLETED; }
}
```

---

## Enums

```java
// ProjectType.java
package com.obracerta.project.domain;

public enum ProjectType {
    HOUSE,
    APARTMENT
}
```

```java
// ProjectStatus.java
package com.obracerta.project.domain;

public enum ProjectStatus {
    IN_BUDGET,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED;

    public boolean canTransitionTo(ProjectStatus target) {
        return switch (this) {
            case IN_BUDGET   -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == ON_HOLD || target == COMPLETED;
            case ON_HOLD     -> target == IN_PROGRESS;
            case COMPLETED   -> false;
        };
    }
}
```

---

## DTOs

```java
// ProjectRequest.java
package com.obracerta.project.dto;

import com.obracerta.project.domain.ProjectType;
import jakarta.validation.constraints.*;

public record ProjectRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    String name,

    @NotNull(message = "Type is required")
    ProjectType type,

    @Size(max = 300, message = "Address must be at most 300 characters")
    String address,

    @Size(max = 500, message = "Description must be at most 500 characters")
    String description
) {}
```

```java
// ProjectResponse.java
package com.obracerta.project.dto;

import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String name,
    ProjectType type,
    String address,
    String description,
    ProjectStatus status,
    BigDecimal totalBudgeted,
    BigDecimal totalSpent,
    BigDecimal remainingBalance,
    BigDecimal spentPercentage,
    BigDecimal completionPercentage,
    boolean overBudget,
    int totalEnvironments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

```java
// StatusUpdateRequest.java
public record StatusUpdateRequest(
    @NotNull(message = "Status is required")
    ProjectStatus status
) {}
```

---

## Repository

```java
package com.obracerta.project.repository;

import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT p FROM Project p
        WHERE p.deletedAt IS NULL
          AND (:status IS NULL OR p.status = :status)
          AND (:type   IS NULL OR p.type   = :type)
          AND (:search IS NULL
               OR LOWER(p.name)    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.address) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Project> findAllWithFilters(
        @Param("status") ProjectStatus status,
        @Param("type")   ProjectType type,
        @Param("search") String search,
        Pageable pageable
    );
}
```

---

# Obra Certa — Angular Spec: Projects Module

## File structure

```
src/app/features/projects/
├── projects.module.ts
├── projects-routing.module.ts
├── pages/
│   ├── project-list/
│   │   ├── project-list.component.ts
│   │   ├── project-list.component.html
│   │   └── project-list.component.scss
│   ├── project-detail/
│   │   ├── project-detail.component.ts
│   │   ├── project-detail.component.html
│   │   └── project-detail.component.scss
│   └── project-form/
│       ├── project-form.component.ts
│       ├── project-form.component.html
│       └── project-form.component.scss
├── components/
│   ├── project-card/
│   │   ├── project-card.component.ts
│   │   └── project-card.component.html
│   └── project-status-badge/
│       ├── project-status-badge.component.ts
│       └── project-status-badge.component.html
└── services/
    └── project.service.ts
```

---

## Routes

```typescript
// projects-routing.module.ts
const routes: Routes = [
  { path: '',           component: ProjectListComponent },
  { path: 'new',        component: ProjectFormComponent },
  { path: ':id',        component: ProjectDetailComponent },
  { path: ':id/edit',   component: ProjectFormComponent },
];
```

---

## Models

```typescript
// src/app/core/models/project.model.ts

export type ProjectType   = 'HOUSE' | 'APARTMENT';
export type ProjectStatus = 'IN_BUDGET' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED';

export interface Project {
  id: number;
  name: string;
  type: ProjectType;
  address?: string;
  description?: string;
  status: ProjectStatus;
  totalBudgeted: number;
  totalSpent: number;
  remainingBalance: number;
  spentPercentage: number;
  completionPercentage: number;
  overBudget: boolean;
  totalEnvironments: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  type: ProjectType;
  address?: string;
  description?: string;
}

export interface ProjectSummary {
  projectId: number;
  name: string;
  status: ProjectStatus;
  totals: {
    budgeted: number;
    spent: number;
    balance: number;
    spentPercentage: number;
    completionPercentage: number;
    overBudget: boolean;
  };
  byEnvironment: {
    environmentId: number;
    name: string;
    budgeted: number;
    spent: number;
    balance: number;
    spentPercentage: number;
    completionPercentage: number;
    overBudget: boolean;
  }[];
  recentExpenses: {
    expenseId: number;
    description: string;
    amount: number;
    date: string;
    environmentName: string;
  }[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

---

## Service

```typescript
// project.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Project, ProjectRequest, ProjectSummary, ProjectStatus,
         ProjectType, PageResponse } from '@core/models/project.model';

export interface ProjectFilters {
  status?: ProjectStatus;
  type?: ProjectType;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/projects`;

  list(filters: ProjectFilters = {}): Observable<PageResponse<Project>> {
    let params = new HttpParams();
    if (filters.status) params = params.set('status', filters.status);
    if (filters.type)   params = params.set('type',   filters.type);
    if (filters.search) params = params.set('search', filters.search);
    if (filters.page != null) params = params.set('page', filters.page);
    if (filters.size != null) params = params.set('size', filters.size);
    if (filters.sort)   params = params.set('sort',   filters.sort);
    return this.http.get<PageResponse<Project>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/${id}`);
  }

  create(data: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, data);
  }

  update(id: number, data: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/${id}`, data);
  }

  updateStatus(id: number, status: ProjectStatus): Observable<Project> {
    return this.http.patch<Project>(`${this.baseUrl}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getSummary(id: number): Observable<ProjectSummary> {
    return this.http.get<ProjectSummary>(`${this.baseUrl}/${id}/summary`);
  }
}
```

---

## Screen 1 — Project List (`project-list`)

### Behavior
- Displays all projects as cards, paginated (20 per page).
- Filters at the top: text search, status filter, type filter.
- "New project" button at the top right.
- Empty state: illustration + "No projects found. Create your first project."
- Loading state: skeleton loader on cards.
- Clicking a card navigates to `/projects/:id`.

### Project card fields (`project-card`)
- Project name
- Status badge (`project-status-badge` component)
- Type: icon + label (house / apartment)
- Address (if filled)
- Progress bar: completion percentage
- Financial summary: total budgeted and total spent
- Over-budget alert icon (red warning) if `overBudget = true`
- Total environments count

### Filters
| Filter | Angular Material Component | Behavior |
|---|---|---|
| Search | `mat-form-field` + `matInput` | 400ms debounce, searches on type |
| Status | `mat-select` | Optional single select |
| Type | `mat-button-toggle-group` | All / House / Apartment |

---

## Screen 2 — Project Form (`project-form`)

### Behavior
- Create mode: title "New project", button "Create project".
- Edit mode: title "Edit project", pre-loads data, button "Save changes".
- On success, redirects to `/projects/:id` with a confirmation snackbar.
- Cancel navigates back.
- Fields are disabled when project status is `COMPLETED`.

### Form fields (Reactive Forms)

| Field | Component | Validation | Notes |
|---|---|---|---|
| Name | `matInput` text | Required, min 3, max 200 | Auto-focus on open |
| Type | `mat-radio-group` | Required | Options: House / Apartment |
| Address | `matInput` text | Max 300 | Optional |
| Description | `matInput` textarea | Max 500 | Optional, 3 rows |

---

## Screen 3 — Project Detail (`project-detail`)

### Behavior
- Header with project name, status badge, and contextual action buttons.
- Financial summary cards (budgeted, spent, balance, completion %).
- Environment list (rendered by the environments module).
- Actions menu: Edit, Change status, Delete.

### Action buttons by status

| Current status | Available actions |
|---|---|
| `IN_BUDGET` | Edit · Start work · Delete |
| `IN_PROGRESS` | Edit · Hold work · Complete work |
| `ON_HOLD` | Edit · Resume work · Complete work |
| `COMPLETED` | View only |

### Required confirmations
- "Delete project": dialog warning all data will be lost.
- "Complete work": dialog with full financial summary before confirming.

---

## Component: `project-status-badge`

| Status | Background | Text color | Label |
|---|---|---|---|
| `IN_BUDGET` | Light blue | Dark blue | In budget |
| `IN_PROGRESS` | Light green | Dark green | In progress |
| `ON_HOLD` | Light yellow | Dark yellow | On hold |
| `COMPLETED` | Light grey | Dark grey | Completed |

```typescript
@Component({
  selector: 'app-project-status-badge',
  template: `<span [class]="'badge badge--' + status.toLowerCase()">{{ label }}</span>`,
})
export class ProjectStatusBadgeComponent {
  @Input({ required: true }) status!: ProjectStatus;

  get label(): string {
    const labels: Record<ProjectStatus, string> = {
      IN_BUDGET:   'In budget',
      IN_PROGRESS: 'In progress',
      ON_HOLD:     'On hold',
      COMPLETED:   'Completed',
    };
    return labels[this.status];
  }
}
```

---

## Notes for Claude Code

1. Use **Signals** (`signal`, `computed`, `effect`) in components instead of `BehaviorSubject`.
2. Use `inject()` instead of constructor injection.
3. Centralised error handling via `HttpInterceptor` — display snackbar with the backend message.
4. `ProjectService` is the single HTTP access point — components never call `HttpClient` directly.
5. Pagination using `MatPaginator` integrated with the paginated backend endpoint.
6. Always use `trackBy` in `*ngFor` for optimised rendering.

---


# Obra Certa — Functional Spec: Environments Module

## Overview

Environments represent the rooms or areas of a project (e.g. kitchen, bathroom, living room). Each project has N environments, and each environment contains the items that will be budgeted and executed.

---

## Permissions by project status

| Project Status | Create env | Edit env | Delete env | Record expense |
|---|---|---|---|---|
| `IN_BUDGET` | ✅ | ✅ | ✅ | ❌ |
| `IN_PROGRESS` | ✅ | ✅ | ❌ | ✅ |
| `ON_HOLD` | ❌ | ✅ | ❌ | ❌ |
| `COMPLETED` | ❌ | ❌ | ❌ | ❌ |

---

## Business rules

### BR-ENV-01 — Creation
- Every environment must belong to an existing, non-deleted project.
- `name` must be unique within the same project.
- `displayOrder` is auto-assigned as the next available number.
- `completionPercentage` starts at 0.

### BR-ENV-02 — Editing
- `name`, `description`, and `displayOrder` can be edited unless the project is `COMPLETED`.
- `completionPercentage` can be manually updated while the project is `IN_PROGRESS`.
- `displayOrder` supports drag-and-drop reordering on the frontend.

### BR-ENV-03 — Deletion
- An environment can only be deleted when the project status is `IN_BUDGET`.
- Deletion is blocked if any of the environment's items have recorded expenses.
- Deletion cascades to all items within the environment.

### BR-ENV-04 — Financial calculations (per environment)
- **Total budgeted:** sum of `(quantity × unitPrice)` across all items.
- **Total spent:** sum of all expenses recorded against the environment's items.
- **Remaining balance:** total budgeted − total spent.
- **Spent percentage:** (total spent / total budgeted) × 100. Returns 0 if total budgeted is 0.
- **Over budget:** `true` if total spent > total budgeted.

### BR-ENV-05 — Completion percentage
- Updated manually by the user (0 to 100).
- The project's overall completion percentage is the average of all its environments.

---

## Main flows

### Flow 1 — Add environment
1. User opens the project detail screen.
2. Clicks "Add environment".
3. Fills in name and optional description.
4. System creates the environment and shows it in the list.

### Flow 2 — Reorder environments
1. User drags an environment card to a new position.
2. System updates the `displayOrder` field for all affected environments via a batch PATCH.

### Flow 3 — Update progress
1. User opens the environment detail.
2. Adjusts the completion percentage slider.
3. System saves and recalculates the project's overall completion.

### Flow 4 — Delete environment
1. User clicks delete.
2. System checks for recorded expenses on any item.
3. If found: displays error "Environment has recorded expenses and cannot be deleted."
4. If not found: displays confirmation and cascades deletion.

---

## Validations

| Field | Rule |
|---|---|
| `name` | Required. Min 2 chars. Max 150 chars. Unique per project. |
| `description` | Optional. Max 300 chars. |
| `displayOrder` | Auto-generated. Positive integer. |
| `completionPercentage` | Numeric. Min 0. Max 100. |

---

# Obra Certa — API Spec: Environments Module

## Base URL

```
/api/v1/projects/{projectId}/environments
/api/v1/environments/{id}
```

---

## Endpoints

### 1. List environments by project

```
GET /api/v1/projects/{projectId}/environments
```

**Response 200:**
```json
[
  {
    "id": 1,
    "projectId": 1,
    "name": "Kitchen",
    "description": "Full remodel with new finishes",
    "displayOrder": 1,
    "completionPercentage": 80.00,
    "totalBudgeted": 22000.00,
    "totalSpent": 18500.00,
    "remainingBalance": 3500.00,
    "spentPercentage": 84.09,
    "overBudget": false,
    "totalItems": 12
  },
  {
    "id": 2,
    "projectId": 1,
    "name": "Bathroom",
    "description": null,
    "displayOrder": 2,
    "completionPercentage": 90.00,
    "totalBudgeted": 15000.00,
    "totalSpent": 15900.00,
    "remainingBalance": -900.00,
    "spentPercentage": 106.00,
    "overBudget": true,
    "totalItems": 8
  }
]
```

---

### 2. Get environment by ID

```
GET /api/v1/environments/{id}
```

**Response 200:**
```json
{
  "id": 1,
  "projectId": 1,
  "name": "Kitchen",
  "description": "Full remodel with new finishes",
  "displayOrder": 1,
  "completionPercentage": 80.00,
  "totalBudgeted": 22000.00,
  "totalSpent": 18500.00,
  "remainingBalance": 3500.00,
  "spentPercentage": 84.09,
  "overBudget": false,
  "totalItems": 12,
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-15T14:30:00"
}
```

---

### 3. Create environment

```
POST /api/v1/projects/{projectId}/environments
```

**Request body:**
```json
{
  "name": "Kitchen",
  "description": "Full remodel with new finishes"
}
```

**Response 201:**
```json
{
  "id": 1,
  "projectId": 1,
  "name": "Kitchen",
  "description": "Full remodel with new finishes",
  "displayOrder": 1,
  "completionPercentage": 0.00,
  "totalBudgeted": 0.00,
  "totalSpent": 0.00,
  "remainingBalance": 0.00,
  "spentPercentage": 0.00,
  "overBudget": false,
  "totalItems": 0,
  "createdAt": "2024-03-20T10:00:00",
  "updatedAt": "2024-03-20T10:00:00"
}
```

**Response 409 — duplicate name:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "An environment named 'Kitchen' already exists in this project.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 4. Update environment

```
PUT /api/v1/environments/{id}
```

**Request body:**
```json
{
  "name": "Open kitchen",
  "description": "Full remodel with island counter"
}
```

**Response 200:** (same schema as GET by ID)

---

### 5. Update completion percentage

```
PATCH /api/v1/environments/{id}/progress
```

**Request body:**
```json
{ "completionPercentage": 75.00 }
```

**Response 200:** (same schema as GET by ID)

---

### 6. Reorder environments

```
PATCH /api/v1/projects/{projectId}/environments/reorder
```

**Request body:**
```json
{
  "orders": [
    { "environmentId": 3, "displayOrder": 1 },
    { "environmentId": 1, "displayOrder": 2 },
    { "environmentId": 2, "displayOrder": 3 }
  ]
}
```

**Response 200:**
```json
{ "message": "Environments reordered successfully." }
```

---

### 7. Delete environment

```
DELETE /api/v1/environments/{id}
```

**Response 204:** (no body)

**Response 409 — has expenses:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "This environment has recorded expenses and cannot be deleted.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

## Endpoint summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/projects/{projectId}/environments` | List environments |
| `GET` | `/api/v1/environments/{id}` | Get environment by ID |
| `POST` | `/api/v1/projects/{projectId}/environments` | Create environment |
| `PUT` | `/api/v1/environments/{id}` | Update environment |
| `PATCH` | `/api/v1/environments/{id}/progress` | Update completion percentage |
| `PATCH` | `/api/v1/projects/{projectId}/environments/reorder` | Reorder environments |
| `DELETE` | `/api/v1/environments/{id}` | Delete environment |

---

# Obra Certa — Entities & Database Spec: Environments Module

## Table: `environment`

| Column | PostgreSQL Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Primary key |
| `project_id` | `BIGINT` | NOT NULL | — | FK → `project(id)` |
| `name` | `VARCHAR(150)` | NOT NULL | — | Environment name |
| `description` | `VARCHAR(300)` | NULL | — | Free text description |
| `display_order` | `INT` | NOT NULL | `0` | Display sort order |
| `completion_percentage` | `NUMERIC(5,2)` | NOT NULL | `0` | 0.00 to 100.00 |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Last update timestamp |

### Constraints

```sql
ALTER TABLE environment
  ADD CONSTRAINT chk_environment_completion
    CHECK (completion_percentage >= 0 AND completion_percentage <= 100);

ALTER TABLE environment
  ADD CONSTRAINT uq_environment_name_project
    UNIQUE (project_id, name);
```

### Indexes

```sql
CREATE INDEX idx_environment_project_id ON environment(project_id);
CREATE INDEX idx_environment_order      ON environment(project_id, display_order);
```

### Flyway migration

```sql
-- V2__create_environment.sql

CREATE TABLE environment (
  id                    BIGSERIAL    PRIMARY KEY,
  project_id            BIGINT       NOT NULL REFERENCES project(id) ON DELETE CASCADE,
  name                  VARCHAR(150) NOT NULL,
  description           VARCHAR(300),
  display_order         INT          NOT NULL DEFAULT 0,
  completion_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
  created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_environment_completion  CHECK (completion_percentage >= 0 AND completion_percentage <= 100),
  CONSTRAINT uq_environment_name_project UNIQUE (project_id, name)
);

CREATE INDEX idx_environment_project_id ON environment(project_id);
CREATE INDEX idx_environment_order      ON environment(project_id, display_order);
```

---

## JPA Entity: `Environment.java`

```java
package com.obracerta.environment.domain;

import com.obracerta.project.domain.Project;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
  name = "environment",
  uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "name"})
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @OneToMany(mappedBy = "environment", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Item> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

## DTOs

```java
// EnvironmentRequest.java
public record EnvironmentRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150)
    String name,

    @Size(max = 300)
    String description
) {}

// EnvironmentResponse.java
public record EnvironmentResponse(
    Long id,
    Long projectId,
    String name,
    String description,
    Integer displayOrder,
    BigDecimal completionPercentage,
    BigDecimal totalBudgeted,
    BigDecimal totalSpent,
    BigDecimal remainingBalance,
    BigDecimal spentPercentage,
    boolean overBudget,
    int totalItems,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// ProgressUpdateRequest.java
public record ProgressUpdateRequest(
    @NotNull
    @DecimalMin("0.0") @DecimalMax("100.0")
    BigDecimal completionPercentage
) {}

// ReorderRequest.java
public record ReorderRequest(
    @NotEmpty List<OrderEntry> orders
) {
    public record OrderEntry(
        @NotNull Long environmentId,
        @NotNull @Min(0) Integer displayOrder
    ) {}
}
```

---

## Repository

```java
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    List<Environment> findByProjectIdOrderByDisplayOrder(Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);

    boolean existsByProjectIdAndNameAndIdNot(Long projectId, String name, Long id);

    @Query("SELECT MAX(e.displayOrder) FROM Environment e WHERE e.project.id = :projectId")
    Optional<Integer> findMaxDisplayOrderByProjectId(@Param("projectId") Long projectId);

    @Query("""
        SELECT COUNT(ex) > 0 FROM Expense ex
        WHERE ex.item.environment.id = :environmentId
        """)
    boolean hasExpenses(@Param("environmentId") Long environmentId);
}
```

---

# Obra Certa — Angular Spec: Environments Module

## File structure

```
src/app/features/environments/
├── environments.module.ts
├── environments-routing.module.ts
├── pages/
│   └── environment-detail/
│       ├── environment-detail.component.ts
│       ├── environment-detail.component.html
│       └── environment-detail.component.scss
├── components/
│   ├── environment-card/
│   │   ├── environment-card.component.ts
│   │   └── environment-card.component.html
│   ├── environment-list/
│   │   ├── environment-list.component.ts
│   │   └── environment-list.component.html
│   └── environment-form-dialog/
│       ├── environment-form-dialog.component.ts
│       └── environment-form-dialog.component.html
└── services/
    └── environment.service.ts
```

---

## Routes

```typescript
const routes: Routes = [
  { path: ':id',      component: EnvironmentDetailComponent },
  { path: ':id/edit', component: EnvironmentDetailComponent },
];
```

> The environment list is rendered inside `project-detail`, not as a standalone route.

---

## Models

```typescript
// src/app/core/models/environment.model.ts

export interface Environment {
  id: number;
  projectId: number;
  name: string;
  description?: string;
  displayOrder: number;
  completionPercentage: number;
  totalBudgeted: number;
  totalSpent: number;
  remainingBalance: number;
  spentPercentage: number;
  overBudget: boolean;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
}

export interface EnvironmentRequest {
  name: string;
  description?: string;
}

export interface ReorderRequest {
  orders: { environmentId: number; displayOrder: number }[];
}
```

---

## Service

```typescript
@Injectable({ providedIn: 'root' })
export class EnvironmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}`;

  listByProject(projectId: number): Observable<Environment[]> {
    return this.http.get<Environment[]>(`${this.baseUrl}/projects/${projectId}/environments`);
  }

  getById(id: number): Observable<Environment> {
    return this.http.get<Environment>(`${this.baseUrl}/environments/${id}`);
  }

  create(projectId: number, data: EnvironmentRequest): Observable<Environment> {
    return this.http.post<Environment>(`${this.baseUrl}/projects/${projectId}/environments`, data);
  }

  update(id: number, data: EnvironmentRequest): Observable<Environment> {
    return this.http.put<Environment>(`${this.baseUrl}/environments/${id}`, data);
  }

  updateProgress(id: number, completionPercentage: number): Observable<Environment> {
    return this.http.patch<Environment>(`${this.baseUrl}/environments/${id}/progress`, { completionPercentage });
  }

  reorder(projectId: number, payload: ReorderRequest): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/projects/${projectId}/environments/reorder`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/environments/${id}`);
  }
}
```

---

## Component: `environment-list` (inside project-detail)

### Behavior
- Renders environments as ordered cards using Angular CDK DragDrop.
- Drag handle visible on hover; reorder calls `PATCH /reorder` on drop.
- "Add environment" button opens `EnvironmentFormDialogComponent`.
- Clicking a card navigates to `/environments/:id`.

### Environment card fields
- Environment name
- Completion progress bar (`mat-progress-bar`)
- Financial mini-summary: budgeted / spent
- Over-budget alert icon if `overBudget = true`
- Total items count
- Action icons: Edit (opens dialog) · Delete (with confirmation)

---

## Component: `environment-form-dialog`

Opens as `MatDialog` for both create and edit.

| Field | Component | Validation |
|---|---|---|
| Name | `matInput` text | Required, min 2, max 150 |
| Description | `matInput` textarea | Optional, max 300 |

- On duplicate name error from API, shows inline field error: "An environment with this name already exists."

---

## Screen: `environment-detail`

### Behavior
- Breadcrumb: Projects > [Project Name] > [Environment Name]
- Summary cards: total budgeted, total spent, remaining balance, completion %
- Editable progress slider (enabled only when project is `IN_PROGRESS`)
- Item table (rendered by the Items module)
- Action buttons: Edit environment · Back to project

### Summary card states
| Card | Normal | Alert |
|---|---|---|
| Remaining balance | Green text | Red text (when negative) |
| Spent % | Blue progress bar | Red progress bar (when > 100%) |

---


# Obra Certa — Functional Spec: Items Module

## Overview

Items are the services or materials that make up an environment's budget. Each item has a quantity, unit of measure, and unit price — forming the budgeted value. Actual expenses are recorded against items.

---

## Item types

| Type | Description | Examples |
|---|---|---|
| `MATERIAL` | Physical product to be purchased | Flooring, paint, cement, fixtures |
| `LABOR` | Service to be performed | Tiling, painting, electrical installation |

---

## Business rules

### BR-ITEM-01 — Creation
- Every item must belong to an existing environment.
- No limit on items per environment.
- Creation is blocked when the project status is `COMPLETED` or `ON_HOLD`.

### BR-ITEM-02 — Editing
- All fields can be edited while the project is not `COMPLETED`.
- Changing `quantity` or `unitPrice` automatically recalculates the environment and project totals.

### BR-ITEM-03 — Deletion
- An item can only be deleted if it has no recorded expenses.
- Deletion is blocked when the project status is `IN_PROGRESS`, `ON_HOLD`, or `COMPLETED`.
- Physical deletion (no soft delete).

### BR-ITEM-04 — Per-item calculations
- **Total budgeted value:** `quantity × unitPrice`
- **Total spent:** sum of all expenses linked to this item.
- **Remaining balance:** total budgeted value − total spent.
- **Over budget:** `true` if total spent > total budgeted value.

### BR-ITEM-05 — Units of measure
The system provides a fixed list of standard units:
`un`, `m`, `m²`, `m³`, `kg`, `L`, `box`, `pc`, `lump`, `hr`, `day`

---

## Main flows

### Flow 1 — Add item
1. User is on the environment detail screen.
2. Clicks "Add item".
3. Fills in description, type, quantity, unit, and unit price.
4. System saves and shows the item in the table with the calculated total.

### Flow 2 — Inline edit
1. User clicks on a cell value in the item table.
2. Cell switches to edit mode.
3. User changes the value and confirms (Enter or blur).
4. System saves and updates the environment totals.

### Flow 3 — Delete item
1. User clicks delete on the item row.
2. System checks for recorded expenses.
3. If found: shows blocking message "This item has recorded expenses and cannot be deleted."
4. If not found: shows confirmation and deletes.

---

## Validations

| Field | Rule |
|---|---|
| `description` | Required. Min 3 chars. Max 300 chars. |
| `type` | Required. `MATERIAL` or `LABOR`. |
| `quantity` | Required. Positive. Max 6 integer digits + 3 decimal places. |
| `unit` | Required. Max 10 chars. |
| `unitPrice` | Required. Positive. Max 10 integer digits + 2 decimal places. |

---

# Obra Certa — API Spec: Items Module

## Base URL

```
/api/v1/environments/{environmentId}/items
/api/v1/items/{id}
```

---

## Endpoints

### 1. List items by environment

```
GET /api/v1/environments/{environmentId}/items
```

**Query params:**

| Param | Type | Description |
|---|---|---|
| `type` | string | Filter by `MATERIAL` or `LABOR` |

**Response 200:**
```json
[
  {
    "id": 1,
    "environmentId": 1,
    "description": "Rectified 60x60 porcelain tile",
    "type": "MATERIAL",
    "quantity": 45.000,
    "unit": "m²",
    "unitPrice": 120.00,
    "totalBudgeted": 5400.00,
    "totalSpent": 5400.00,
    "remainingBalance": 0.00,
    "overBudget": false
  },
  {
    "id": 2,
    "environmentId": 1,
    "description": "Tile installation",
    "type": "LABOR",
    "quantity": 45.000,
    "unit": "m²",
    "unitPrice": 35.00,
    "totalBudgeted": 1575.00,
    "totalSpent": 1700.00,
    "remainingBalance": -125.00,
    "overBudget": true
  }
]
```

---

### 2. Get item by ID

```
GET /api/v1/items/{id}
```

**Response 200:**
```json
{
  "id": 1,
  "environmentId": 1,
  "environmentName": "Kitchen",
  "projectId": 1,
  "description": "Rectified 60x60 porcelain tile",
  "type": "MATERIAL",
  "quantity": 45.000,
  "unit": "m²",
  "unitPrice": 120.00,
  "totalBudgeted": 5400.00,
  "totalSpent": 5400.00,
  "remainingBalance": 0.00,
  "overBudget": false,
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-10T08:00:00"
}
```

---

### 3. Create item

```
POST /api/v1/environments/{environmentId}/items
```

**Request body:**
```json
{
  "description": "Rectified 60x60 porcelain tile",
  "type": "MATERIAL",
  "quantity": 45.000,
  "unit": "m²",
  "unitPrice": 120.00
}
```

**Response 201:**
```json
{
  "id": 1,
  "environmentId": 1,
  "environmentName": "Kitchen",
  "projectId": 1,
  "description": "Rectified 60x60 porcelain tile",
  "type": "MATERIAL",
  "quantity": 45.000,
  "unit": "m²",
  "unitPrice": 120.00,
  "totalBudgeted": 5400.00,
  "totalSpent": 0.00,
  "remainingBalance": 5400.00,
  "overBudget": false,
  "createdAt": "2024-03-20T10:00:00",
  "updatedAt": "2024-03-20T10:00:00"
}
```

---

### 4. Update item

```
PUT /api/v1/items/{id}
```

**Request body:** (same schema as POST)

**Response 200:** (same schema as GET by ID)

**Response 409:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Items cannot be edited when the project status is COMPLETED.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 5. Delete item

```
DELETE /api/v1/items/{id}
```

**Response 204:** (no body)

**Response 409 — has expenses:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "This item has recorded expenses and cannot be deleted.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 6. List available units

```
GET /api/v1/items/units
```

**Response 200:**
```json
["un", "m", "m²", "m³", "kg", "L", "box", "pc", "lump", "hr", "day"]
```

---

## Endpoint summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/environments/{environmentId}/items` | List items by environment |
| `GET` | `/api/v1/items/{id}` | Get item by ID |
| `POST` | `/api/v1/environments/{environmentId}/items` | Create item |
| `PUT` | `/api/v1/items/{id}` | Update item |
| `DELETE` | `/api/v1/items/{id}` | Delete item |
| `GET` | `/api/v1/items/units` | List available units of measure |

---

# Obra Certa — Entities & Database Spec: Items Module

## Table: `item`

| Column | PostgreSQL Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Primary key |
| `environment_id` | `BIGINT` | NOT NULL | — | FK → `environment(id)` |
| `description` | `VARCHAR(300)` | NOT NULL | — | Item description |
| `type` | `VARCHAR(10)` | NOT NULL | — | `MATERIAL` or `LABOR` |
| `quantity` | `NUMERIC(9,3)` | NOT NULL | — | Quantity |
| `unit` | `VARCHAR(10)` | NOT NULL | — | Unit of measure |
| `unit_price` | `NUMERIC(12,2)` | NOT NULL | — | Unit price |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Last update timestamp |

### Constraints

```sql
ALTER TABLE item
  ADD CONSTRAINT chk_item_type     CHECK (type IN ('MATERIAL', 'LABOR'));

ALTER TABLE item
  ADD CONSTRAINT chk_item_quantity  CHECK (quantity   > 0);

ALTER TABLE item
  ADD CONSTRAINT chk_item_price     CHECK (unit_price > 0);
```

### Indexes

```sql
CREATE INDEX idx_item_environment_id ON item(environment_id);
CREATE INDEX idx_item_type           ON item(type);
```

### Flyway migration

```sql
-- V3__create_item.sql

CREATE TABLE item (
  id             BIGSERIAL     PRIMARY KEY,
  environment_id BIGINT        NOT NULL REFERENCES environment(id) ON DELETE CASCADE,
  description    VARCHAR(300)  NOT NULL,
  type           VARCHAR(10)   NOT NULL,
  quantity       NUMERIC(9,3)  NOT NULL,
  unit           VARCHAR(10)   NOT NULL,
  unit_price     NUMERIC(12,2) NOT NULL,
  created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_item_type     CHECK (type       IN ('MATERIAL', 'LABOR')),
  CONSTRAINT chk_item_quantity CHECK (quantity   > 0),
  CONSTRAINT chk_item_price    CHECK (unit_price > 0)
);

CREATE INDEX idx_item_environment_id ON item(environment_id);
CREATE INDEX idx_item_type           ON item(type);
```

---

## JPA Entity: `Item.java`

```java
package com.obracerta.item.domain;

import com.obracerta.environment.domain.Environment;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(nullable = false, length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemType type;

    @Column(nullable = false, precision = 9, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Expense> expenses = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getTotalBudgeted() {
        return quantity.multiply(unitPrice);
    }
}
```

---

## Enum

```java
// ItemType.java
package com.obracerta.item.domain;

public enum ItemType {
    MATERIAL,
    LABOR
}
```

---

## DTOs

```java
// ItemRequest.java
public record ItemRequest(
    @NotBlank(message = "Description is required")
    @Size(min = 3, max = 300)
    String description,

    @NotNull(message = "Type is required")
    ItemType type,

    @NotNull @Positive
    @Digits(integer = 6, fraction = 3)
    BigDecimal quantity,

    @NotBlank
    @Size(max = 10)
    String unit,

    @NotNull @Positive
    @Digits(integer = 10, fraction = 2)
    BigDecimal unitPrice
) {}

// ItemResponse.java
public record ItemResponse(
    Long id,
    Long environmentId,
    String environmentName,
    Long projectId,
    String description,
    ItemType type,
    BigDecimal quantity,
    String unit,
    BigDecimal unitPrice,
    BigDecimal totalBudgeted,
    BigDecimal totalSpent,
    BigDecimal remainingBalance,
    boolean overBudget,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

---

## Repository

```java
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByEnvironmentIdOrderByCreatedAt(Long environmentId);

    List<Item> findByEnvironmentIdAndType(Long environmentId, ItemType type);

    @Query("SELECT COUNT(e) > 0 FROM Expense e WHERE e.item.id = :itemId")
    boolean hasExpenses(@Param("itemId") Long itemId);

    @Query("""
        SELECT COALESCE(SUM(i.quantity * i.unitPrice), 0)
        FROM Item i WHERE i.environment.project.id = :projectId
        """)
    BigDecimal sumTotalBudgetedByProjectId(@Param("projectId") Long projectId);

    @Query("""
        SELECT COALESCE(SUM(i.quantity * i.unitPrice), 0)
        FROM Item i WHERE i.environment.id = :environmentId
        """)
    BigDecimal sumTotalBudgetedByEnvironmentId(@Param("environmentId") Long environmentId);
}
```

---

# Obra Certa — Angular Spec: Items Module

## File structure

```
src/app/features/items/
├── items.module.ts
├── components/
│   ├── item-table/
│   │   ├── item-table.component.ts
│   │   ├── item-table.component.html
│   │   └── item-table.component.scss
│   └── item-form-dialog/
│       ├── item-form-dialog.component.ts
│       └── item-form-dialog.component.html
└── services/
    └── item.service.ts
```

> Items have no standalone route — they are always rendered inside `environment-detail`.

---

## Models

```typescript
// src/app/core/models/item.model.ts

export type ItemType = 'MATERIAL' | 'LABOR';

export interface Item {
  id: number;
  environmentId: number;
  environmentName: string;
  projectId: number;
  description: string;
  type: ItemType;
  quantity: number;
  unit: string;
  unitPrice: number;
  totalBudgeted: number;
  totalSpent: number;
  remainingBalance: number;
  overBudget: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ItemRequest {
  description: string;
  type: ItemType;
  quantity: number;
  unit: string;
  unitPrice: number;
}
```

---

## Service

```typescript
@Injectable({ providedIn: 'root' })
export class ItemService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}`;

  listByEnvironment(environmentId: number, type?: ItemType): Observable<Item[]> {
    let params = new HttpParams();
    if (type) params = params.set('type', type);
    return this.http.get<Item[]>(`${this.baseUrl}/environments/${environmentId}/items`, { params });
  }

  getById(id: number): Observable<Item> {
    return this.http.get<Item>(`${this.baseUrl}/items/${id}`);
  }

  create(environmentId: number, data: ItemRequest): Observable<Item> {
    return this.http.post<Item>(`${this.baseUrl}/environments/${environmentId}/items`, data);
  }

  update(id: number, data: ItemRequest): Observable<Item> {
    return this.http.put<Item>(`${this.baseUrl}/items/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/items/${id}`);
  }

  getUnits(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/items/units`);
  }
}
```

---

## Component: `item-table`

### Behavior
- Rendered inside `environment-detail`.
- Displays all items as a `mat-table` with columns: description, type, quantity, unit, unit price, total budgeted, total spent, remaining balance, actions.
- "Add item" button above the table opens `ItemFormDialogComponent`.
- Over-budget rows are highlighted with a light red background.
- Inline editing: clicking a numeric cell (quantity, unit price) activates an inline input.
- Type filter: `mat-button-toggle-group` above the table — All / Material / Labor.

### Table columns

| Column | Display | Notes |
|---|---|---|
| `description` | Description | Truncated at 40 chars with tooltip |
| `type` | Type | Chip: Material (blue) / Labor (orange) |
| `quantity` | Qty | Inline editable |
| `unit` | Unit | Read-only |
| `unitPrice` | Unit price | Inline editable, formatted as currency |
| `totalBudgeted` | Budgeted | Calculated, read-only |
| `totalSpent` | Spent | Read-only |
| `remainingBalance` | Balance | Green if positive, red if negative |
| actions | — | Edit icon · Delete icon |

---

## Component: `item-form-dialog`

Opens as `MatDialog` for both create and edit.

| Field | Component | Validation | Notes |
|---|---|---|---|
| Description | `matInput` text | Required, min 3, max 300 | Auto-focus |
| Type | `mat-radio-group` | Required | Material / Labor |
| Quantity | `matInput` number | Required, positive | Shows calculated total live |
| Unit | `mat-select` | Required | Populated from `GET /items/units` |
| Unit price | `matInput` number | Required, positive | Shows calculated total live |

### Live total preview
While filling quantity and unit price, the dialog shows:
> **Budgeted total: R$ 5,400.00**

This value updates on every keystroke.

---


# Obra Certa — Functional Spec: Expenses Module

## Overview

Expenses represent the actual money spent during the renovation. Each expense is recorded against a specific item and can optionally have a category and a receipt attachment. Expenses are the core of the "budgeted vs. actual" comparison.

---

## Business rules

### BR-EXP-01 — Creation
- Every expense must be linked to an existing item.
- Expense recording is only allowed when the project status is `IN_PROGRESS`.
- There is no limit on the number of expenses per item.
- Expenses can exceed the item's budgeted value — the system flags it but does not block it.

### BR-EXP-02 — Editing
- `amount`, `date`, `description`, `categoryId`, and `receiptUrl` can be edited while the project is `IN_PROGRESS`.
- Editing is blocked when the project is `ON_HOLD`, `COMPLETED`, or `IN_BUDGET`.

### BR-EXP-03 — Deletion
- An expense can be deleted while the project is `IN_PROGRESS`.
- Deletion is physical (no soft delete).
- After deletion, totals for the item, environment, and project are recalculated.

### BR-EXP-04 — Categories
- Categories are optional and pre-seeded in the database.
- Default categories: `Materials`, `Labor`, `Transport`, `Tools`, `Fees`, `Other`.
- Users cannot create or delete categories (read-only list for now).

### BR-EXP-05 — Receipt
- Receipt is an optional URL string (uploaded externally or typed).
- Max 500 characters.

### BR-EXP-06 — Date
- `date` is the actual date the expense occurred (not necessarily today).
- Cannot be a future date.

---

## Main flows

### Flow 1 — Record expense
1. User is on the environment detail screen, on a specific item row.
2. Clicks "Record expense" on the item.
3. Fills in amount, date, description (optional), category (optional), receipt URL (optional).
4. System saves and updates all financial totals up the chain (item → environment → project).

### Flow 2 — List expenses per item
1. User expands an item row (accordion or side panel).
2. System loads all expenses for that item ordered by date descending.
3. User can edit or delete individual expenses.

### Flow 3 — Delete expense
1. User clicks delete on an expense.
2. System shows a confirmation dialog.
3. User confirms. Expense is deleted and totals recalculated.

---

## Validations

| Field | Rule |
|---|---|
| `amount` | Required. Positive. Max 10 integer digits + 2 decimal places. |
| `date` | Required. Cannot be a future date. |
| `description` | Optional. Max 300 chars. |
| `categoryId` | Optional. Must be a valid category ID. |
| `receiptUrl` | Optional. Max 500 chars. |
| `itemId` | Required. Must exist and belong to a project with status `IN_PROGRESS`. |

---

# Obra Certa — API Spec: Expenses Module

## Base URL

```
/api/v1/items/{itemId}/expenses
/api/v1/expenses/{id}
/api/v1/categories
```

---

## Endpoints

### 1. List expenses by item

```
GET /api/v1/items/{itemId}/expenses
```

**Query params:**

| Param | Type | Description |
|---|---|---|
| `categoryId` | long | Filter by category |
| `from` | date | Filter from date (ISO: `yyyy-MM-dd`) |
| `to` | date | Filter to date (ISO: `yyyy-MM-dd`) |

**Response 200:**
```json
[
  {
    "id": 1,
    "itemId": 1,
    "itemDescription": "Rectified 60x60 porcelain tile",
    "environmentName": "Kitchen",
    "category": { "id": 1, "name": "Materials", "color": "#4A90D9" },
    "amount": 3200.00,
    "date": "2024-03-18",
    "description": "Purchase at Leroy Merlin",
    "receiptUrl": "https://storage.obracerta.com/receipts/nf-001.pdf",
    "createdAt": "2024-03-18T14:00:00"
  }
]
```

---

### 2. List expenses by project (for dashboard feed)

```
GET /api/v1/projects/{projectId}/expenses
```

**Query params:**

| Param | Type | Description |
|---|---|---|
| `environmentId` | long | Filter by environment |
| `categoryId` | long | Filter by category |
| `from` | date | From date |
| `to` | date | To date |
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 20) |

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "itemId": 1,
      "itemDescription": "Rectified 60x60 porcelain tile",
      "environmentName": "Kitchen",
      "category": { "id": 1, "name": "Materials", "color": "#4A90D9" },
      "amount": 3200.00,
      "date": "2024-03-18",
      "description": "Purchase at Leroy Merlin",
      "receiptUrl": null,
      "createdAt": "2024-03-18T14:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

---

### 3. Get expense by ID

```
GET /api/v1/expenses/{id}
```

**Response 200:** (same schema as list item, full object)

---

### 4. Create expense

```
POST /api/v1/items/{itemId}/expenses
```

**Request body:**
```json
{
  "amount": 3200.00,
  "date": "2024-03-18",
  "description": "Purchase at Leroy Merlin",
  "categoryId": 1,
  "receiptUrl": "https://storage.obracerta.com/receipts/nf-001.pdf"
}
```

**Response 201:**
```json
{
  "id": 1,
  "itemId": 1,
  "itemDescription": "Rectified 60x60 porcelain tile",
  "environmentName": "Kitchen",
  "category": { "id": 1, "name": "Materials", "color": "#4A90D9" },
  "amount": 3200.00,
  "date": "2024-03-18",
  "description": "Purchase at Leroy Merlin",
  "receiptUrl": "https://storage.obracerta.com/receipts/nf-001.pdf",
  "createdAt": "2024-03-18T14:00:00"
}
```

**Response 409 — project not in progress:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Expenses can only be recorded when the project status is IN_PROGRESS.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 5. Update expense

```
PUT /api/v1/expenses/{id}
```

**Request body:** (same schema as POST, without `itemId`)

**Response 200:** (same schema as GET by ID)

---

### 6. Delete expense

```
DELETE /api/v1/expenses/{id}
```

**Response 204:** (no body)

---

### 7. List categories

```
GET /api/v1/categories
```

**Response 200:**
```json
[
  { "id": 1, "name": "Materials", "color": "#4A90D9" },
  { "id": 2, "name": "Labor",     "color": "#7ED321" },
  { "id": 3, "name": "Transport", "color": "#F5A623" },
  { "id": 4, "name": "Tools",     "color": "#9B59B6" },
  { "id": 5, "name": "Fees",      "color": "#E74C3C" },
  { "id": 6, "name": "Other",     "color": "#95A5A6" }
]
```

---

## Endpoint summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/items/{itemId}/expenses` | List expenses by item |
| `GET` | `/api/v1/projects/{projectId}/expenses` | List expenses by project (paginated) |
| `GET` | `/api/v1/expenses/{id}` | Get expense by ID |
| `POST` | `/api/v1/items/{itemId}/expenses` | Create expense |
| `PUT` | `/api/v1/expenses/{id}` | Update expense |
| `DELETE` | `/api/v1/expenses/{id}` | Delete expense |
| `GET` | `/api/v1/categories` | List categories |

---

# Obra Certa — Entities & Database Spec: Expenses Module

## Table: `category`

| Column | PostgreSQL Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Primary key |
| `name` | `VARCHAR(100)` | NOT NULL | — | Category name |
| `color` | `VARCHAR(7)` | NOT NULL | — | Hex color code |

### Flyway migration + seed

```sql
-- V4__create_category.sql

CREATE TABLE category (
  id    BIGSERIAL    PRIMARY KEY,
  name  VARCHAR(100) NOT NULL UNIQUE,
  color VARCHAR(7)   NOT NULL
);

INSERT INTO category (name, color) VALUES
  ('Materials', '#4A90D9'),
  ('Labor',     '#7ED321'),
  ('Transport', '#F5A623'),
  ('Tools',     '#9B59B6'),
  ('Fees',      '#E74C3C'),
  ('Other',     '#95A5A6');
```

---

## Table: `expense`

| Column | PostgreSQL Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Primary key |
| `item_id` | `BIGINT` | NOT NULL | — | FK → `item(id)` |
| `category_id` | `BIGINT` | NULL | — | FK → `category(id)` |
| `amount` | `NUMERIC(12,2)` | NOT NULL | — | Expense amount |
| `date` | `DATE` | NOT NULL | — | Date expense occurred |
| `description` | `VARCHAR(300)` | NULL | — | Optional description |
| `receipt_url` | `VARCHAR(500)` | NULL | — | Optional receipt URL |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | `NOW()` | Last update timestamp |

### Constraints

```sql
ALTER TABLE expense
  ADD CONSTRAINT chk_expense_amount CHECK (amount > 0);
```

### Indexes

```sql
CREATE INDEX idx_expense_item_id     ON expense(item_id);
CREATE INDEX idx_expense_category_id ON expense(category_id);
CREATE INDEX idx_expense_date        ON expense(date DESC);
```

### Flyway migration

```sql
-- V5__create_expense.sql

CREATE TABLE expense (
  id           BIGSERIAL     PRIMARY KEY,
  item_id      BIGINT        NOT NULL REFERENCES item(id) ON DELETE CASCADE,
  category_id  BIGINT        REFERENCES category(id) ON DELETE SET NULL,
  amount       NUMERIC(12,2) NOT NULL,
  date         DATE          NOT NULL,
  description  VARCHAR(300),
  receipt_url  VARCHAR(500),
  created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP     NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_expense_amount CHECK (amount > 0)
);

CREATE INDEX idx_expense_item_id     ON expense(item_id);
CREATE INDEX idx_expense_category_id ON expense(category_id);
CREATE INDEX idx_expense_date        ON expense(date DESC);
```

---

## JPA Entity: `Category.java`

```java
package com.obracerta.category.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;
}
```

---

## JPA Entity: `Expense.java`

```java
package com.obracerta.expense.domain;

import com.obracerta.category.domain.Category;
import com.obracerta.item.domain.Item;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 300)
    private String description;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

## DTOs

```java
// ExpenseRequest.java
public record ExpenseRequest(
    @NotNull @Positive
    @Digits(integer = 10, fraction = 2)
    BigDecimal amount,

    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Date cannot be in the future")
    LocalDate date,

    @Size(max = 300)
    String description,

    Long categoryId,

    @Size(max = 500)
    String receiptUrl
) {}

// ExpenseResponse.java
public record ExpenseResponse(
    Long id,
    Long itemId,
    String itemDescription,
    String environmentName,
    CategoryResponse category,
    BigDecimal amount,
    LocalDate date,
    String description,
    String receiptUrl,
    LocalDateTime createdAt
) {}

// CategoryResponse.java
public record CategoryResponse(
    Long id,
    String name,
    String color
) {}
```

---

## Repositories

```java
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByItemIdOrderByDateDesc(Long itemId);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.item.environment.project.id = :projectId
          AND (:environmentId IS NULL OR e.item.environment.id = :environmentId)
          AND (:categoryId    IS NULL OR e.category.id          = :categoryId)
          AND (:from          IS NULL OR e.date                >= :from)
          AND (:to            IS NULL OR e.date                <= :to)
        ORDER BY e.date DESC
        """)
    Page<Expense> findByProjectWithFilters(
        @Param("projectId")     Long projectId,
        @Param("environmentId") Long environmentId,
        @Param("categoryId")    Long categoryId,
        @Param("from")          LocalDate from,
        @Param("to")            LocalDate to,
        Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e WHERE e.item.id = :itemId
        """)
    BigDecimal sumAmountByItemId(@Param("itemId") Long itemId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e WHERE e.item.environment.id = :environmentId
        """)
    BigDecimal sumAmountByEnvironmentId(@Param("environmentId") Long environmentId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e WHERE e.item.environment.project.id = :projectId
        """)
    BigDecimal sumAmountByProjectId(@Param("projectId") Long projectId);
}

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();
}
```

---

# Obra Certa — Angular Spec: Expenses Module

## File structure

```
src/app/features/expenses/
├── expenses.module.ts
├── pages/
│   └── expense-list/
│       ├── expense-list.component.ts
│       ├── expense-list.component.html
│       └── expense-list.component.scss
├── components/
│   ├── expense-form-dialog/
│   │   ├── expense-form-dialog.component.ts
│   │   └── expense-form-dialog.component.html
│   └── expense-item-panel/
│       ├── expense-item-panel.component.ts
│       └── expense-item-panel.component.html
└── services/
    ├── expense.service.ts
    └── category.service.ts
```

---

## Models

```typescript
// src/app/core/models/expense.model.ts

export interface Category {
  id: number;
  name: string;
  color: string;
}

export interface Expense {
  id: number;
  itemId: number;
  itemDescription: string;
  environmentName: string;
  category?: Category;
  amount: number;
  date: string;
  description?: string;
  receiptUrl?: string;
  createdAt: string;
}

export interface ExpenseRequest {
  amount: number;
  date: string;
  description?: string;
  categoryId?: number;
  receiptUrl?: string;
}

export interface ExpenseFilters {
  environmentId?: number;
  categoryId?: number;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
```

---

## Services

```typescript
@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}`;

  listByItem(itemId: number): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.baseUrl}/items/${itemId}/expenses`);
  }

  listByProject(projectId: number, filters: ExpenseFilters = {}): Observable<PageResponse<Expense>> {
    let params = new HttpParams();
    if (filters.environmentId) params = params.set('environmentId', filters.environmentId);
    if (filters.categoryId)    params = params.set('categoryId',    filters.categoryId);
    if (filters.from)          params = params.set('from',          filters.from);
    if (filters.to)            params = params.set('to',            filters.to);
    if (filters.page != null)  params = params.set('page',          filters.page);
    if (filters.size != null)  params = params.set('size',          filters.size);
    return this.http.get<PageResponse<Expense>>(
      `${this.baseUrl}/projects/${projectId}/expenses`, { params }
    );
  }

  create(itemId: number, data: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>(`${this.baseUrl}/items/${itemId}/expenses`, data);
  }

  update(id: number, data: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`${this.baseUrl}/expenses/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/expenses/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Category[]> {
    return this.http.get<Category[]>(`${environment.apiUrl}/categories`);
  }
}
```

---

## Component: `expense-item-panel`

Shown as an expandable accordion row under each item in `item-table`.

### Behavior
- Expands on click of the item row's expand icon.
- Loads expenses lazily on first expand (not on component init).
- Displays a compact list of expenses: date · category chip · amount · description · receipt icon.
- "Record expense" button at the top of the panel opens `ExpenseFormDialogComponent`.
- Edit and delete icons per expense row.

### Expense row fields
| Field | Display |
|---|---|
| `date` | Formatted: `dd MMM yyyy` |
| `category` | Colored chip with category name |
| `amount` | Currency formatted |
| `description` | Truncated at 50 chars with tooltip |
| `receiptUrl` | Paperclip icon (links to URL if present) |
| actions | Edit icon · Delete icon |

---

## Component: `expense-form-dialog`

Opens as `MatDialog` for both create and edit.

| Field | Component | Validation | Notes |
|---|---|---|---|
| Amount | `matInput` number | Required, positive | Currency mask |
| Date | `matDatepicker` | Required, not future | |
| Description | `matInput` text | Optional, max 300 | |
| Category | `mat-select` | Optional | Populated from `CategoryService` |
| Receipt URL | `matInput` text | Optional, max 500 | Shows link preview if filled |

---

## Screen: `expense-list` (standalone page per project)

Accessible from the project detail via "View all expenses" button.

### Behavior
- Full paginated list of all expenses for the project.
- Filters: environment (select), category (select), date range (date pickers).
- Sortable columns: date, amount.
- Totals footer: total shown in current filter + grand total.
- Each row links to the parent item/environment.

---


# Obra Certa — Full Spec: Dashboard Module

---

## 1. Functional Spec

### Overview
The dashboard is a read-only view that consolidates a project's financial and progress data into a single, visually rich screen. It is fed entirely by `GET /api/v1/projects/{id}/summary`.

### Business rules

#### BR-DASH-01 — Data source
- All dashboard data comes from a single summary endpoint to avoid multiple round trips.
- Data reflects the current state in real time (no caching on the frontend).

#### BR-DASH-02 — Over-budget indicators
- Any environment where `totalSpent > totalBudgeted` is highlighted in red.
- The project-level alert fires when the project's `overBudget = true`.

#### BR-DASH-03 — Empty states
- If the project has no environments: show "Add environments to start tracking your budget."
- If the project has environments but no expenses: show financial totals as 0 with a prompt to start recording expenses.

#### BR-DASH-04 — Access
- The dashboard is accessible for any project status.
- `COMPLETED` projects show a "Completed" banner at the top.

---

## 2. API Spec

The dashboard consumes one endpoint:

```
GET /api/v1/projects/{id}/summary
```

Full response documented in the Projects API spec (`02-api.md`).

Additionally, for the expenses feed on the dashboard:

```
GET /api/v1/projects/{projectId}/expenses?page=0&size=5&sort=date,desc
```

This returns the 5 most recent expenses across all environments.

---

## 3. Angular Spec

### File structure

```
src/app/features/dashboard/
├── dashboard.module.ts
├── dashboard-routing.module.ts
├── pages/
│   └── project-dashboard/
│       ├── project-dashboard.component.ts
│       ├── project-dashboard.component.html
│       └── project-dashboard.component.scss
└── components/
    ├── summary-cards/
    │   ├── summary-cards.component.ts
    │   └── summary-cards.component.html
    ├── budget-vs-spent-chart/
    │   ├── budget-vs-spent-chart.component.ts
    │   └── budget-vs-spent-chart.component.html
    ├── environment-progress-list/
    │   ├── environment-progress-list.component.ts
    │   └── environment-progress-list.component.html
    └── recent-expenses-feed/
        ├── recent-expenses-feed.component.ts
        └── recent-expenses-feed.component.html
```

### Route

```typescript
{ path: 'projects/:id/dashboard', component: ProjectDashboardComponent }
```

### Models

```typescript
// Reuses ProjectSummary from project.model.ts
// No additional models needed
```

---

## 4. UI Components

### Summary cards (`summary-cards`)

Four cards displayed in a responsive grid (2×2 on mobile, 4×1 on desktop):

| Card | Value | Color rule |
|---|---|---|
| Total budgeted | `totals.budgeted` formatted as currency | Always neutral |
| Total spent | `totals.spent` formatted as currency | Red if `overBudget`, else neutral |
| Remaining balance | `totals.balance` formatted as currency | Green if positive, red if negative |
| Completion | `totals.completionPercentage`% with circular progress | Blue |

Each card also has a subtle secondary metric:
- Budgeted card → number of environments
- Spent card → spent percentage (e.g. "38% of budget")
- Balance card → trend vs. last week (future feature placeholder)
- Completion card → estimated finish date (future feature placeholder)

---

### Budget vs. Spent chart (`budget-vs-spent-chart`)

**Type:** Grouped bar chart (Chart.js `bar`)

**Data:** One group per environment — two bars each: budgeted (blue) and spent (green or red if over budget).

**Chart config:**
```typescript
const chartData = {
  labels: summary.byEnvironment.map(e => e.name),
  datasets: [
    {
      label: 'Budgeted',
      data: summary.byEnvironment.map(e => e.budgeted),
      backgroundColor: '#4A90D9',
    },
    {
      label: 'Spent',
      data: summary.byEnvironment.map(e => e.spent),
      backgroundColor: summary.byEnvironment.map(e =>
        e.overBudget ? '#E74C3C' : '#7ED321'
      ),
    },
  ],
};

const chartOptions = {
  responsive: true,
  plugins: {
    legend: { position: 'top' },
    tooltip: {
      callbacks: {
        label: (ctx) => `R$ ${ctx.raw.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
      },
    },
  },
  scales: {
    y: { ticks: { callback: (val) => `R$ ${val.toLocaleString('pt-BR')}` } },
  },
};
```

---

### Environment progress list (`environment-progress-list`)

A vertical list of environments, each showing:
- Environment name
- `mat-progress-bar` for completion percentage (value: `completionPercentage`)
- Mini financial summary: budgeted / spent / balance
- Over-budget chip (red, shown only if `overBudget = true`)
- Clicking the row navigates to `/environments/:id`

**Visual states:**

| State | Progress bar color | Balance color |
|---|---|---|
| Under budget | Green | Green |
| Over budget | Red | Red |
| No expenses yet | Grey | Neutral |

---

### Recent expenses feed (`recent-expenses-feed`)

A compact list of the 5 most recent expenses across the project.

Each row:
- Date (formatted `dd MMM`)
- Category chip (colored)
- Environment name
- Expense description (truncated)
- Amount (right-aligned, bold)

Footer: "View all expenses →" link navigates to `/projects/:id/expenses`.

---

## 5. Layout

```
┌──────────────────────────────────────────────────────────┐
│  [Project Name]              [Status badge]  [Edit btn]  │
├──────────┬──────────┬──────────┬────────────────────────-┤
│ Budgeted │  Spent   │ Balance  │     Completion %        │
│ R$85,000 │ R$32,400 │ R$52,600 │       ◎ 35%             │
├──────────┴──────────┴──────────┴─────────────────────────┤
│                Budget vs. Spent by Environment            │
│  [Grouped bar chart — full width]                        │
├──────────────────────────────┬───────────────────────────┤
│  Environment Progress        │  Recent Expenses           │
│  ─────────────────────────   │  ─────────────────────     │
│  Kitchen          ████░ 80%  │  18 Mar  Materials  3,200  │
│  Bathroom  ⚠️     █████ 90%  │  17 Mar  Labor      1,500  │
│  Living room      ██░░░ 40%  │  15 Mar  Tools        320  │
│  Master bedroom   █░░░░ 20%  │  14 Mar  Materials  2,100  │
│  Hall             ░░░░░  0%  │  12 Mar  Labor        800  │
│                              │  → View all expenses       │
└──────────────────────────────┴───────────────────────────┘
```

---

## 6. Notes for Claude Code

1. Load the full summary in `ngOnInit` via `ProjectService.getSummary(id)` and store in a Signal.
2. The Chart.js chart must be destroyed and recreated if the project ID changes (use `effect()` reacting to the Signal).
3. Use `@defer` (Angular 17+) for the chart component to lazy-load Chart.js only when the dashboard is opened.
4. All currency values formatted with Angular's `CurrencyPipe` using locale `pt-BR` and currency `BRL`.
5. The dashboard must work on mobile — stack all sections vertically on screens < 768px.
6. Completion percentage ring uses a CSS-only donut (no extra library needed).

