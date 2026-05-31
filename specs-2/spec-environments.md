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

