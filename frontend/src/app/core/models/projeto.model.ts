export type ProjectType = 'HOUSE' | 'APARTMENT';

export type ProjectStatus =
  | 'IN_BUDGET'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED';

export interface Project {
  id: number;
  name: string;
  type: ProjectType;
  address?: string;
  description?: string;
  status: ProjectStatus;
  totalBudget: number;
  totalActual: number;
  remainingBalance: number;
  actualPercentage: number;
  completionPercentage: number;
  overBudget: boolean;
  hasDelayedItems: boolean;
  roomCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  type: ProjectType;
  address?: string;
  description?: string;
}

export interface StatusRequest {
  status: ProjectStatus;
}

export interface ProjectSummary {
  projectId: number;
  name: string;
  status: ProjectStatus;
  totals: {
    budget: number;
    actual: number;
    balance: number;
    actualPercentage: number;
    completionPercentage: number;
    overBudget: boolean;
  };
  byEnvironment: {
    environmentId: number;
    name: string;
    budget: number;
    actual: number;
    balance: number;
    actualPercentage: number;
    completionPercentage: number;
    overBudget: boolean;
  }[];
  recentExpenses: {
    expenseId: number;
    description: string;
    amount: number;
    paidAt: string | null;
    environmentName: string;
  }[];
}

export interface Environment {
  id: number;
  projectId: number;
  name: string;
  description?: string;
  completionPercentage: number;
  hasDelayedItems: boolean;
}

export interface EnvironmentRequest {
  name: string;
  description?: string;
  completionPercentage?: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProjectFilters {
  status?: ProjectStatus;
  type?: ProjectType;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ─── Item ───────────────────────────────────────────────────────────────────
export type ItemType = 'MATERIAL' | 'LABOR';

export interface Item {
  id: number;
  environmentId: number;
  description: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  type: ItemType;
  totalBudgeted: number;
  totalActual: number;
  completed: boolean;
  delayed: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ItemFlagsRequest {
  completed?: boolean;
  delayed?: boolean;
}

export interface ItemRequest {
  description: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  type: ItemType;
}

// ─── Category ────────────────────────────────────────────────────────────────
export interface Category {
  id: number;
  name: string;
  color: string;
}

// ─── Expense ─────────────────────────────────────────────────────────────────
export interface Expense {
  id: number;
  itemId: number;
  categoryId: number | null;
  categoryName: string | null;
  amount: number;
  paidAt: string | null;
  description: string | null;
  receiptUrl: string | null;
  createdAt: string;
}

export interface ExpenseRequest {
  categoryId?: number;
  amount: number;
  paidAt?: string;
  description?: string;
  receiptUrl?: string;
}
