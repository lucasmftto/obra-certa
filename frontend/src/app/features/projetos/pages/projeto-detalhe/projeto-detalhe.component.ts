import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrlPipe } from '@core/pipes/brl.pipe';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProjectService } from '@core/services/projeto.service';
import { EnvironmentService } from '@core/services/environment.service';
import { ProjectStatusBadgeComponent } from '../../components/projeto-status-badge/projeto-status-badge.component';
import { Project, ProjectStatus, Environment } from '@core/models/projeto.model';

interface AcaoStatus {
  label: string;
  novoStatus: ProjectStatus;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-projeto-detalhe',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressBarModule,
    MatDividerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    ProjectStatusBadgeComponent,
    BrlPipe,
  ],
  templateUrl: './projeto-detalhe.component.html',
  styleUrls: ['./projeto-detalhe.component.scss'],
})
export class ProjectDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private environmentService = inject(EnvironmentService);
  private snackBar = inject(MatSnackBar);

  projeto = signal<Project | null>(null);
  ambientes = signal<Environment[]>([]);
  carregando = signal(true);
  carregandoAmbientes = signal(true);

  podeEditar    = computed(() => this.projeto()?.status !== 'COMPLETED');
  podeExcluir   = computed(() => this.projeto()?.status === 'IN_BUDGET');
  podeAdicionarAmbiente = computed(() =>
    this.projeto()?.status !== 'COMPLETED'
  );

  acoesPorStatus = computed<AcaoStatus[]>(() => {
    const s = this.projeto()?.status;
    if (!s) return [];

    const mapa: Record<ProjectStatus, AcaoStatus[]> = {
      IN_BUDGET: [
        { label: 'Iniciar obra', novoStatus: 'IN_PROGRESS', icon: 'play_arrow', color: 'primary' },
      ],
      IN_PROGRESS: [
        { label: 'Pausar obra', novoStatus: 'ON_HOLD', icon: 'pause', color: 'accent' },
        { label: 'Concluir obra', novoStatus: 'COMPLETED', icon: 'check_circle', color: 'primary' },
      ],
      ON_HOLD: [
        { label: 'Retomar obra', novoStatus: 'IN_PROGRESS', icon: 'play_arrow', color: 'primary' },
        { label: 'Concluir obra', novoStatus: 'COMPLETED', icon: 'check_circle', color: 'primary' },
      ],
      COMPLETED: [
        { label: 'Reabrir obra', novoStatus: 'IN_PROGRESS', icon: 'restart_alt', color: 'accent' },
      ],
    };

    return mapa[s] ?? [];
  });

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.projectService.findById(id).subscribe({
      next: (p) => {
        this.projeto.set(p);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.router.navigate(['/projects']);
      },
    });
    this.environmentService.listByProject(id).subscribe({
      next: (envs) => {
        this.ambientes.set(envs);
        this.carregandoAmbientes.set(false);
      },
      error: () => this.carregandoAmbientes.set(false),
    });
  }

  mudarStatus(novoStatus: ProjectStatus): void {
    const id = this.projeto()!.id;
    this.projectService.updateStatus(id, novoStatus).subscribe({
      next: (p) => {
        this.projeto.set(p);
        this.snackBar.open('Status atualizado!', 'OK', { duration: 3000 });
      },
      error: () => {},
    });
  }

  excluir(): void {
    if (!confirm('Tem certeza? Todos os dados do projeto serão removidos.')) return;
    this.projectService.delete(this.projeto()!.id).subscribe({
      next: () => {
        this.snackBar.open('Projeto excluído.', 'OK', { duration: 3000 });
        this.router.navigate(['/projects']);
      },
    });
  }
}
