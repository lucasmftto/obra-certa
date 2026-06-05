import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BrlMaskDirective } from '@core/pipes/brl-mask.directive';
import { ExpenseService } from '@core/services/expense.service';
import { CategoryService } from '@core/services/category.service';
import { Category } from '@core/models/projeto.model';

@Component({
  selector: 'app-expense-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDatepickerModule, MatNativeDateModule, MatIconModule, MatCardModule,
    MatSnackBarModule, BrlMaskDirective,
  ],
  templateUrl: './expense-form.component.html',
  styleUrls: ['./expense-form.component.scss'],
})
export class ExpenseFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private expenseService = inject(ExpenseService);
  private categoryService = inject(CategoryService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  projectId = signal(0);
  environmentId = signal(0);
  itemId = signal(0);
  categories = signal<Category[]>([]);
  submitting = signal(false);
  maxDate = new Date();

  form = this.fb.group({
    categoryId: [null as number | null],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    paidAt: [null as Date | null],
    description: ['', Validators.maxLength(300)],
  });

  ngOnInit() {
    this.projectId.set(+this.route.snapshot.paramMap.get('id')!);
    this.environmentId.set(+this.route.snapshot.paramMap.get('envId')!);
    this.itemId.set(+this.route.snapshot.paramMap.get('itemId')!);
    this.categoryService.listAll().subscribe({ next: cats => this.categories.set(cats), error: () => {} });
  }

  save() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    const v = this.form.getRawValue();

    const toDateStr = (d: Date | null): string | undefined =>
      d ? d.toISOString().split('T')[0] : undefined;

    this.expenseService.create(this.itemId(), {
      categoryId: v.categoryId ?? undefined,
      amount: v.amount!,
      paidAt: toDateStr(v.paidAt),
      description: v.description || undefined,
    }).subscribe({
      next: () => {
        this.snackBar.open('Gasto registrado!', 'OK', { duration: 3000 });
        this.router.navigate(['/projects', this.projectId(), 'environments', this.environmentId()]);
      },
      error: () => this.submitting.set(false),
    });
  }

  back() {
    this.router.navigate(['/projects', this.projectId(), 'environments', this.environmentId()]);
  }
}
