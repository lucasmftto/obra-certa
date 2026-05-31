import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectStatus } from '@core/models/projeto.model';

@Component({
  selector: 'app-projeto-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span [class]="badgeClass()">{{ label() }}</span>`,
  styles: [`
    span {
      display: inline-flex;
      align-items: center;
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.3px;
      white-space: nowrap;
    }
    .badge-in-budget   { background: #EBF4FF; color: #1A6BB5; border: 1px solid #BDD9F5; }
    .badge-in-progress { background: #EDFBF3; color: #1E8A4C; border: 1px solid #B5E8CC; }
    .badge-on-hold     { background: #FFF5E6; color: #C46A00; border: 1px solid #FDDCAC; }
    .badge-completed   { background: #F0EBE8; color: #6D4C41; border: 1px solid #D9CECA; }
  `],
})
export class ProjectStatusBadgeComponent {
  status = input.required<ProjectStatus>();

  label = computed(() => ({
    IN_BUDGET:   'Em orçamento',
    IN_PROGRESS: 'Em andamento',
    ON_HOLD:     'Pausado',
    COMPLETED:   'Concluído',
  }[this.status()]));

  badgeClass = computed(() => 'badge-' + this.status().toLowerCase().replace(/_/g, '-'));
}
