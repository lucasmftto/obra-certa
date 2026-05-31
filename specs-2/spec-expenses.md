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

