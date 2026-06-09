# Obra Certa — Full Spec: Final Project Report

## 1. Functional Spec

### Overview
The final report is a full-page HTML view that consolidates all financial, progress, and expense data for a project. It is designed to be beautiful on screen and clean when printed or saved as PDF via the browser's native print dialog.

### Access rules
- Accessible for any project status via a "View report" button on the project detail screen.
- The report is read-only. No editing actions.
- URL: `/projects/:id/report`

### Business rules

#### BR-REP-01 — Data source
- All data loaded from a single endpoint: `GET /api/v1/projects/{id}/report-data`.
- No caching — always fresh on page open.

#### BR-REP-02 — Print behavior
- "Print / Save PDF" button at top right (hidden on print).
- Before calling `window.print()`, convert each Chart.js canvas to base64 via `chart.toBase64Image()` and set as `src` on a sibling `<img class="chart-print-img">`. Then call `window.print()`. After `window.afterprint` fires, hide the images and show the canvases again.
- Print CSS hides topbar, sidebar, and print button.
- Each environment section (item table) breaks to a new page — except the first.
- `canvas` hidden on print; `.chart-print-img` shown on print.

#### BR-REP-03 — Over-budget highlighting
- Project level: if `totals.overBudget = true`, show red alert banner above KPI cards.
- Environment level: badge turns red if `environment.overBudget = true`.
- Item level: balance cell is red if negative, green if positive, muted grey if zero.

#### BR-REP-04 — Empty states
- No expenses on an item: spent column shows `R$0`, balance matches budgeted (green).
- No expenses at all: expense feed section is hidden entirely.

---

## 2. API Spec

### Single endpoint for the entire report

```
GET /api/v1/projects/{projectId}/report-data
```

**Response:**
```json
{
  "projectId": 1,
  "name": "Moema apartment renovation",
  "address": "Rua das Flores, 123 — Moema, SP",
  "type": "APARTMENT",
  "status": "COMPLETED",
  "completionDate": "2024-04-18",
  "generatedAt": "2024-06-07T10:00:00",
  "totals": {
    "budgeted": 85000.00,
    "spent": 91200.00,
    "balance": -6200.00,
    "spentPercentage": 107.29,
    "completionPercentage": 100.00,
    "overBudget": true,
    "totalMaterialsBudgeted": 52700.00,
    "totalMaterialsSpent": 56500.00,
    "totalLaborBudgeted": 32300.00,
    "totalLaborSpent": 34700.00
  },
  "environments": [
    {
      "id": 1,
      "name": "Kitchen",
      "displayOrder": 1,
      "completionPercentage": 100.00,
      "budgeted": 22000.00,
      "spent": 19800.00,
      "balance": 2200.00,
      "overBudget": false,
      "items": [
        {
          "id": 1,
          "description": "Rectified 60x60 porcelain tile",
          "type": "MATERIAL",
          "quantity": 45.000,
          "unit": "m²",
          "unitPrice": 120.00,
          "totalBudgeted": 5400.00,
          "totalSpent": 5400.00,
          "balance": 0.00,
          "overBudget": false
        }
      ]
    }
  ],
  "expensesByDate": [
    { "date": "2024-02-12", "amount": 2400.00, "runningTotal": 2400.00 },
    { "date": "2024-02-20", "amount": 1200.00, "runningTotal": 3600.00 }
  ],
  "allExpenses": [
    {
      "id": 1,
      "date": "2024-04-15",
      "description": "Final touch-up paint",
      "amount": 820.00,
      "environmentName": "Hall",
      "category": { "id": 2, "name": "Labor", "color": "#7ED321" },
      "receiptUrl": null
    }
  ]
}
```

### Backend implementation

Create `ReportController.java` and `ReportService.java`. The service must assemble all data in a single JPQL query with joins across `project → environment → item → expense`. Use DTO projections — do not load full entities to avoid N+1.

`expensesByDate` must be pre-sorted ascending by date with a running cumulative total computed in the service, not in the frontend.

```java
@GetMapping("/api/v1/projects/{projectId}/report-data")
public ResponseEntity<ProjectReportResponse> getReportData(@PathVariable Long projectId) {
    return ResponseEntity.ok(reportService.buildReport(projectId));
}
```

---

## 3. Angular Spec

### File structure

```
src/app/features/report/
├── report.module.ts
├── report-routing.module.ts
├── pages/
│   └── project-report/
│       ├── project-report.component.ts
│       ├── project-report.component.html
│       └── project-report.component.scss
├── components/
│   ├── report-cover/
│   ├── report-kpi-cards/
│   ├── report-env-bars/
│   ├── report-charts/
│   ├── report-item-tables/
│   └── report-expense-feed/
└── services/
    └── report.service.ts
```

### Route

```typescript
{ path: 'projects/:id/report', component: ProjectReportComponent }
```

### Models

```typescript
export interface ReportTotals {
  budgeted: number;
  spent: number;
  balance: number;
  spentPercentage: number;
  completionPercentage: number;
  overBudget: boolean;
  totalMaterialsBudgeted: number;
  totalMaterialsSpent: number;
  totalLaborBudgeted: number;
  totalLaborSpent: number;
}

export interface ReportItem {
  id: number;
  description: string;
  type: 'MATERIAL' | 'LABOR';
  quantity: number;
  unit: string;
  unitPrice: number;
  totalBudgeted: number;
  totalSpent: number;
  balance: number;
  overBudget: boolean;
}

export interface ReportEnvironment {
  id: number;
  name: string;
  displayOrder: number;
  completionPercentage: number;
  budgeted: number;
  spent: number;
  balance: number;
  overBudget: boolean;
  items: ReportItem[];
}

export interface ReportExpense {
  id: number;
  date: string;
  description: string;
  amount: number;
  environmentName: string;
  category: { id: number; name: string; color: string } | null;
  receiptUrl: string | null;
}

export interface ProjectReportData {
  projectId: number;
  name: string;
  address: string;
  type: 'HOUSE' | 'APARTMENT';
  status: string;
  completionDate: string | null;
  generatedAt: string;
  totals: ReportTotals;
  environments: ReportEnvironment[];
  expensesByDate: { date: string; amount: number; runningTotal: number }[];
  allExpenses: ReportExpense[];
}
```

### Service

```typescript
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  getReportData(projectId: number): Observable<ProjectReportData> {
    return this.http.get<ProjectReportData>(
      `${environment.apiUrl}/projects/${projectId}/report-data`
    );
  }
}
```

### Component behavior

```typescript
export class ProjectReportComponent implements OnInit, AfterViewInit {
  private reportService = inject(ReportService);
  private route = inject(ActivatedRoute);

  report = signal<ProjectReportData | null>(null);
  loading = signal(true);

  // Chart instances — kept for print conversion
  private charts: Chart[] = [];

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.reportService.getReportData(id).subscribe({
      next: data => { this.report.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  ngAfterViewInit() {
    // Charts are initialised after report signal emits — use effect()
  }

  printReport() {
    // 1. Convert each chart canvas to base64 image
    this.charts.forEach(chart => {
      const img = document.getElementById(`${chart.id}-img`) as HTMLImageElement;
      if (img) img.src = chart.toBase64Image();
    });
    // 2. Small delay to ensure images are painted
    setTimeout(() => window.print(), 100);
    // 3. Restore canvases after printing
    window.addEventListener('afterprint', () => {
      this.charts.forEach(chart => {
        const img = document.getElementById(`${chart.id}-img`) as HTMLImageElement;
        if (img) img.src = '';
      });
    }, { once: true });
  }
}
```

### Shared pipe

```typescript
// brl-currency.pipe.ts
@Pipe({ name: 'brl', standalone: true })
export class BrlCurrencyPipe implements PipeTransform {
  transform(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency', currency: 'BRL', maximumFractionDigits: 0
    }).format(value);
  }
}
```

Use `{{ value | brl }}` throughout the report template.

---

## 4. Report Sections & UI Spec

### Fonts
- Body: `DM Sans` (Google Fonts) — weights 300, 400, 500, 600
- Numbers / monospaced values: `DM Mono` — weights 400, 500

### Color tokens (hardcoded — Chart.js cannot read CSS vars)

| Token | Hex | Usage |
|---|---|---|
| `--blue` | `#1B4FD8` | Primary, budgeted bars, Material chip |
| `--blue-lt` | `#EEF3FD` | Material chip bg, KPI card bg |
| `--blue-md` | `#6B94F0` | Cover eyebrow, receipt icon |
| `--green` | `#177B4A` | Under-budget bars, positive balance |
| `--green-lt` | `#E8F5EE` | Under-budget badge bg |
| `--red` | `#C0392B` | Over-budget bars, negative balance, alert |
| `--red-lt` | `#FDEEEC` | Over-budget badge bg, alert bg |
| `--amber` | `#B05A00` | Labor chip text |
| `--amber-lt` | `#FEF3E6` | Labor chip bg |
| `--ink` | `#0E1117` | Primary text |
| `--ink-2` | `#3A3F4B` | Secondary text |
| `--ink-3` | `#7A8090` | Muted/label text |
| `--rule` | `#E4E7EF` | Borders, dividers |
| `--surface` | `#F7F8FC` | Page bg, card inner bg |

---

### Section 0 — Top bar (hidden on print)

```
[← Back to project]     [Report title]     [🖨 Print / Save PDF]
```

- Sticky, `backdrop-filter: blur(10px)`, bg `rgba(247,248,252,0.92)`.
- Print button: blue bg, white text, calls `printReport()`.

---

### Section 1 — Cover

Full-width card with `background: #1B4FD8`, `border-radius: 16px`.

Decorative circles: two `border-radius: 50%` pseudo-elements with `rgba(255,255,255,0.05)` — top-right and bottom-right.

| Element | Detail |
|---|---|
| Eyebrow | `"Obra Certa · Final Report"` — 11px, letter-spacing 2px, color `#6B94F0` |
| Title | Project name — 32px, weight 600, white |
| Subtitle | `"{address} · Completed {date}"` or `"In progress"` — 14px, `rgba(255,255,255,0.55)` |
| 4 stat pills | Grid 4 cols, `background: rgba(255,255,255,0.1)`, border `rgba(255,255,255,0.15)` |

Stat pill value colors:
- Budgeted → white
- Total spent → `#FF9B8E` if `overBudget`, else white
- Balance → `#FF9B8E` if negative, `#7BE4A8` if positive
- Completion → `#7BE4A8`

---

### Section 2 — Financial summary

Red alert banner (if `overBudget`):
> ⚠️ This project exceeded the budget by R${amount} ({percentage}%).

4 KPI cards in a row. Each: `background: var(--surface)`, 1px border, `border-radius: 10px`.

| Card | Value color | Sub-text |
|---|---|---|
| Total budgeted | `--blue` | `{n} environments · {n} items` |
| Total spent | `--red` if over, else `--blue` | `{spentPercentage}% of budget` |
| Over / Under budget | `--red` / `--green` | "over budget" / "under budget" |
| Materials vs. labor | `--blue` | `R$XX,XXX · R$XX,XXX` |

---

### Section 3 — Budget vs. spent by environment

**Chart:** Grouped horizontal bar (Chart.js `bar`, `indexAxis: 'y'`), height 260px.

```typescript
datasets: [
  {
    label: 'Budgeted',
    backgroundColor: '#D6E4FB',   // blue-lt variant
    borderRadius: 4,
    borderSkipped: false,
  },
  {
    label: 'Spent',
    backgroundColor: environments.map(e => e.overBudget ? '#E24B4A' : '#177B4A'),
    borderRadius: 4,
    borderSkipped: false,
  },
]
```

Below chart — one row per environment:
- Environment name (bold)
- Badge: green "−X% under" or red "+X% over"
- Monospaced: `{budgeted} budgeted · {spent} spent`
- Double-layer progress bar: light blue track (budget) + green/red fill (spent), height 10px

---

### Section 4 — Breakdown analysis (2-column grid)

**Left — Material vs. labor donut:**
- Chart.js `doughnut`, `cutout: '65%'`, height 160px
- Colors: Materials `#1B4FD8`, Labor `#F59E0B`
- Legend: hide Chart.js legend; show custom table beside the chart

Legend table columns: Category · Budgeted · Spent · Balance
- Balance: green if positive, red if negative

**Right — Spending over time line chart:**
- Chart.js `line`, height 200px
- Dataset 1: cumulative spent — `borderColor: '#1B4FD8'`, `fill: true`, `backgroundColor: 'rgba(27,79,216,0.07)'`, `tension: 0.35`
- Dataset 2: budget limit — `borderColor: '#C0392B'`, `borderDash: [5,3]`, no fill, no points
- Y-axis ticks formatted with `BrlCurrencyPipe` equivalent

---

### Section 5 — Item detail by environment

One sub-section per environment, no wrapper card — all inside the parent section card.

**Environment sub-header:**
```
[name bold]  [progress bar 80px]  [XX%]  [badge: Under/Over budget]
```

**Item table columns:** Description · Type · Qty · Unit price · Budgeted · Spent · Balance

- Type chip: `MATERIAL` → blue bg/text, `LABOR` → amber bg/text
- Balance: green if > 0, red if < 0, muted grey if = 0
- Last row: bold total row with grey background

Print: `page-break-before: always` on every env sub-section except the first.

---

### Section 6 — Expense feed

Expenses grouped by month (descending). Month separator shows month name + total.

**Each expense row:**
```
[8px colored dot]  [description + env·category]  [amount bold mono]  [DD MMM]  [📋 if receipt]
```

- Dot color: `category.color` from API
- Description truncated with `text-overflow: ellipsis`
- Amount right-aligned, `DM Mono`, bold
- Receipt icon: only shown if `receiptUrl` is not null

---

## 5. Print CSS

```scss
@media print {
  .topbar,
  app-topbar,
  app-sidebar,
  .print-btn {
    display: none !important;
  }

  body { background: white; font-size: 11pt; }
  .report { padding: 0; max-width: 100%; }
  .cover { border-radius: 0; }

  .env-section { page-break-before: always; }
  .env-section:first-of-type { page-break-before: avoid; }

  canvas { display: none !important; }
  .chart-print-img { display: block !important; width: 100%; }

  a[href]::after { content: none; }
}
```

---

## 6. Notes for Claude Code

1. **Single API call** — `report-data` returns everything. No secondary calls needed.
2. **`@defer` for Chart.js** — lazy-load the chart module so it doesn't bloat the initial bundle.
3. **Chart registration** — with Chart.js 4, import and register only the components used:
   ```typescript
   import { Chart, BarController, DoughnutController, LineController,
            CategoryScale, LinearScale, BarElement, ArcElement,
            PointElement, LineElement, Legend, Tooltip, Filler } from 'chart.js';
   Chart.register(...);
   ```
4. **`BrlCurrencyPipe`** — create as a standalone pipe, use `maximumFractionDigits: 0` for whole numbers in the report.
5. **Chart color arrays** — all hex hardcoded; Chart.js cannot read CSS variables.
6. **Print flow** — `printReport()` converts canvases → base64 images → calls `window.print()` with 100ms delay → on `afterprint` event, clears image `src`.
7. **Mobile** — charts stack vertically, item tables use `overflow-x: auto` wrapper.
8. **Font loading** — add to `index.html`:
   ```html
   <link href="https://fonts.googleapis.com/css2?family=DM+Sans:opsz,wght@9..40,300;9..40,400;9..40,500;9..40,600&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
   ```
9. **Prototype reference** — the file `obra-certa-report.html` is the visual reference. Match it exactly: colors, spacing, font sizes, chart configs, and print behavior.
