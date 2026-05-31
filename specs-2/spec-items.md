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

