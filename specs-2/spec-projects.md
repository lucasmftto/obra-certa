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

