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
