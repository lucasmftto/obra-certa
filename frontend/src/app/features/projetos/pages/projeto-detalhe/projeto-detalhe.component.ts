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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ProjectService } from '@core/services/projeto.service';
import { EnvironmentService } from '@core/services/environment.service';
import { AttachmentService } from '@core/services/attachment.service';
import { AuthService } from '@core/services/auth.service';
import { ProjectStatusBadgeComponent } from '../../components/projeto-status-badge/projeto-status-badge.component';
import { AttachmentDialogComponent } from '../../components/attachment-dialog/attachment-dialog.component';
import { MemberDialogComponent } from '../../components/member-dialog/member-dialog.component';
import { Project, ProjectStatus, Environment } from '@core/models/projeto.model';

interface StatusAction {
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
    MatDialogModule,
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
  private attachmentService = inject(AttachmentService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  project = signal<Project | null>(null);
  environments = signal<Environment[]>([]);
  totalAttachments = signal(0);
  loading = signal(true);
  loadingEnvironments = signal(true);

  canEdit    = computed(() => this.project()?.status !== 'COMPLETED');
  canDelete  = computed(() => this.project()?.status === 'IN_BUDGET');
  canAddEnvironment = computed(() => this.project()?.status !== 'COMPLETED');
  isOwner    = computed(() =>
    this.project()?.ownerId === this.authService.currentUser()?.id
  );

  statusActions = computed<StatusAction[]>(() => {
    const s = this.project()?.status;
    if (!s) return [];

    const mapa: Record<ProjectStatus, StatusAction[]> = {
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
        this.project.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/projects']);
      },
    });
    this.environmentService.listByProject(id).subscribe({
      next: (envs) => {
        this.environments.set(envs);
        this.loadingEnvironments.set(false);
      },
      error: () => this.loadingEnvironments.set(false),
    });
    this.attachmentService.list(id).subscribe({
      next: (lista) => this.totalAttachments.set(lista.length),
      error: () => {},
    });
  }

  openMembers(): void {
    this.dialog.open(MemberDialogComponent, {
      data: {
        projectId: this.project()!.id,
        ownerId: this.project()!.ownerId,
        currentUserId: this.authService.currentUser()!.id,
      },
      width: '560px',
    });
  }

  openAttachments(): void {
    const ref = this.dialog.open(AttachmentDialogComponent, {
      data: { projectId: this.project()!.id },
      width: '560px',
      maxHeight: '80vh',
      panelClass: 'attachment-dialog-panel',
    });
    ref.afterClosed().subscribe((count: number) => {
      if (count !== undefined) this.totalAttachments.set(count);
    });
  }

  changeStatus(novoStatus: ProjectStatus): void {
    const id = this.project()!.id;
    this.projectService.updateStatus(id, novoStatus).subscribe({
      next: (p) => {
        this.project.set(p);
        this.snackBar.open('Status atualizado!', 'OK', { duration: 3000 });
      },
      error: () => {},
    });
  }

  delete(): void {
    if (!confirm('Tem certeza? Todos os dados do projeto serão removidos.')) return;
    this.projectService.delete(this.project()!.id).subscribe({
      next: () => {
        this.snackBar.open('Projeto excluído.', 'OK', { duration: 3000 });
        this.router.navigate(['/projects']);
      },
    });
  }
}
