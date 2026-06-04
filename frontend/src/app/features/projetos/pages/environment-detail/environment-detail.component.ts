import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrlPipe } from '@core/pipes/brl.pipe';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ItemService } from '@core/services/item.service';
import { ProjectService } from '@core/services/projeto.service';
import { EnvironmentService } from '@core/services/environment.service';
import { Environment, Item, Project } from '@core/models/projeto.model';

@Component({
  selector: 'app-environment-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, BrlPipe,
    MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule,
    MatChipsModule, MatTableModule, MatSnackBarModule, MatTooltipModule,
    MatCheckboxModule,
  ],
  templateUrl: './environment-detail.component.html',
  styleUrls: ['./environment-detail.component.scss'],
})
export class EnvironmentDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private itemService = inject(ItemService);
  private projectService = inject(ProjectService);
  private environmentService = inject(EnvironmentService);
  private snackBar = inject(MatSnackBar);

  projectId = signal(0);
  environmentId = signal(0);
  project = signal<Project | null>(null);
  environment = signal<Environment | null>(null);
  items = signal<Item[]>([]);
  carregando = signal(true);

  totalBudgeted = computed(() => this.items().reduce((s, i) => s + i.totalBudgeted, 0));
  totalActual   = computed(() => this.items().reduce((s, i) => s + i.totalActual, 0));

  completedCount = computed(() => this.items().filter(i => i.completed).length);

  completionPercentage = computed(() => {
    const total = this.items().length;
    if (total === 0) return 0;
    return Math.round((this.completedCount() / total) * 100);
  });

  displayedColumns = ['status', 'description', 'type', 'quantity', 'unitPrice', 'budgeted', 'actual', 'actions'];

  ngOnInit() {
    const pid = +this.route.snapshot.paramMap.get('id')!;
    const eid = +this.route.snapshot.paramMap.get('envId')!;
    this.projectId.set(pid);
    this.environmentId.set(eid);

    this.projectService.findById(pid).subscribe({ next: p => this.project.set(p), error: () => {} });
    this.environmentService.findById(eid).subscribe({ next: e => this.environment.set(e), error: () => {} });
    this.itemService.listByEnvironment(eid).subscribe({
      next: items => { this.items.set(items); this.carregando.set(false); },
      error: () => this.carregando.set(false),
    });
  }

  toggleCompleted(item: Item) {
    const newValue = !item.completed;
    this.items.update(list => list.map(i => i.id === item.id ? { ...i, completed: newValue } : i));
    this.itemService.updateFlags(item.id, { completed: newValue }).subscribe({
      error: () => {
        this.items.update(list => list.map(i => i.id === item.id ? { ...i, completed: item.completed } : i));
        this.snackBar.open('Erro ao atualizar item.', 'OK', { duration: 3000 });
      },
    });
  }

  toggleDelayed(item: Item) {
    const newValue = !item.delayed;
    this.items.update(list => list.map(i => i.id === item.id ? { ...i, delayed: newValue } : i));
    this.itemService.updateFlags(item.id, { delayed: newValue }).subscribe({
      error: () => {
        this.items.update(list => list.map(i => i.id === item.id ? { ...i, delayed: item.delayed } : i));
        this.snackBar.open('Erro ao atualizar item.', 'OK', { duration: 3000 });
      },
    });
  }

  deleteItem(id: number) {
    if (!confirm('Excluir este item e todos os gastos vinculados?')) return;
    this.itemService.delete(id).subscribe({
      next: () => {
        this.items.update(list => list.filter(i => i.id !== id));
        this.snackBar.open('Item excluído.', 'OK', { duration: 3000 });
      },
      error: () => {},
    });
  }
}
