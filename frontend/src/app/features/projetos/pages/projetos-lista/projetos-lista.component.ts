import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime } from 'rxjs/operators';
import { ProjectService } from '@core/services/projeto.service';
import { ProjectCardComponent } from '../../components/projeto-card/projeto-card.component';
import { Project, ProjectStatus, ProjectType } from '@core/models/projeto.model';

@Component({
  selector: 'app-projetos-lista',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonToggleModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    ProjectCardComponent,
  ],
  templateUrl: './projetos-lista.component.html',
  styleUrls: ['./projetos-lista.component.scss'],
})
export class ProjectListComponent implements OnInit {
  private projectService = inject(ProjectService);

  projects = signal<Project[]>([]);
  totalElements = signal(0);
  loading = signal(false);

  page = signal(0);
  size = signal(20);
  filtroStatus = signal<ProjectStatus | undefined>(undefined);
  filtroTipo = signal<ProjectType | undefined>(undefined);

  buscaControl = new FormControl('');

  ngOnInit(): void {
    this.loadProjects();
    this.buscaControl.valueChanges.pipe(debounceTime(400)).subscribe(() => {
      this.page.set(0);
      this.loadProjects();
    });
  }

  loadProjects(): void {
    this.loading.set(true);
    this.projectService
      .list({
        status: this.filtroStatus(),
        type: this.filtroTipo(),
        search: this.buscaControl.value ?? undefined,
        page: this.page(),
        size: this.size(),
        sort: 'createdAt,desc',
      })
      .subscribe({
        next: (result) => {
          this.projects.set(result.content);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  onStatusChange(status: ProjectStatus | ''): void {
    this.filtroStatus.set(status || undefined);
    this.page.set(0);
    this.loadProjects();
  }

  onTipoChange(tipo: ProjectType | 'TODOS'): void {
    this.filtroTipo.set(tipo === 'TODOS' ? undefined : tipo);
    this.page.set(0);
    this.loadProjects();
  }

  onPageChange(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
    this.loadProjects();
  }
}
