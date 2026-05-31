import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrlPipe } from '@core/pipes/brl.pipe';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProjectStatusBadgeComponent } from '../projeto-status-badge/projeto-status-badge.component';
import { Project } from '@core/models/projeto.model';

@Component({
  selector: 'app-projeto-card',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatCardModule, MatIconModule,
    MatProgressBarModule, MatButtonModule, MatTooltipModule,
    ProjectStatusBadgeComponent, BrlPipe,
  ],
  templateUrl: './projeto-card.component.html',
  styles: [`
    .projeto-card {
      height: 100%;
      display: flex;
      flex-direction: column;
      transition: box-shadow 0.25s ease, transform 0.2s ease;
      cursor: pointer;
    }
    .projeto-card:hover {
      box-shadow: var(--shadow-hover) !important;
      transform: translateY(-2px);
    }
    .projeto-card.em-alerta {
      border-left: 4px solid #E53935 !important;
    }
    .card-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text);
    }
    .card-title span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .header-badges {
      display: flex;
      align-items: center;
      gap: 4px;
      flex-shrink: 0;
    }
    mat-card-subtitle {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 4px;
      color: var(--color-text-muted) !important;
    }
    .tipo-icon {
      font-size: 16px;
      height: 16px;
      width: 16px;
      color: var(--color-text-muted);
    }
    .progresso-label {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: var(--color-text-muted);
      margin-bottom: 6px;
      margin-top: 12px;
    }
    .financeiro {
      display: flex;
      justify-content: space-between;
      margin-top: 16px;
      gap: 8px;
      padding-top: 12px;
      border-top: 1px solid var(--color-border);
    }
    .financeiro-item {
      display: flex;
      flex-direction: column;
      gap: 3px;
    }
    .financeiro-item .label {
      font-size: 10px;
      color: var(--color-text-muted);
      text-transform: uppercase;
      letter-spacing: 0.6px;
      font-weight: 500;
    }
    .financeiro-item .valor {
      font-size: 15px;
      font-weight: 600;
      color: var(--color-text);
    }
    .financeiro-item .valor.text-warn { color: #E53935; }
    mat-card-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 16px;
      margin-top: auto;
      border-top: 1px solid var(--color-border);
    }
    .ambientes-count {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: var(--color-text-muted);
    }
    .ambientes-count mat-icon {
      font-size: 16px;
      height: 16px;
      width: 16px;
    }
  `],
})
export class ProjectCardComponent {
  projeto = input.required<Project>();
}
