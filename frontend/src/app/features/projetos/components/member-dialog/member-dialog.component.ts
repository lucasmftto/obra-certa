import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MemberService } from '@core/services/member.service';
import { MemberResponse, MemberRole } from '@core/models/member.model';

export interface MemberDialogData {
  projectId: number;
  ownerId: number;
  currentUserId: number;
}

@Component({
  selector: 'app-member-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './member-dialog.component.html',
  styleUrls: ['./member-dialog.component.scss'],
})
export class MemberDialogComponent implements OnInit {
  private dialogRef = inject(MatDialogRef<MemberDialogComponent>);
  private data: MemberDialogData = inject(MAT_DIALOG_DATA);
  private memberService = inject(MemberService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  readonly projectId = this.data.projectId;
  readonly isOwner = this.data.ownerId === this.data.currentUserId;

  members = signal<MemberResponse[]>([]);
  loading = signal(true);
  submitting = signal(false);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['EDITOR' as MemberRole, Validators.required],
  });

  ngOnInit(): void {
    this.loadMembers();
  }

  loadMembers(): void {
    this.loading.set(true);
    this.memberService.list(this.projectId).subscribe({
      next: (list) => {
        this.members.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  addMember(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.memberService.add(this.projectId, this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.form.reset({ email: '', role: 'EDITOR' });
        this.snackBar.open('Membro adicionado!', 'OK', { duration: 3000 });
        this.loadMembers();
      },
      error: () => this.submitting.set(false),
    });
  }

  removeMember(member: MemberResponse): void {
    if (!confirm(`Remover ${member.name} do projeto?`)) return;
    this.memberService.remove(this.projectId, member.userId).subscribe({
      next: () => {
        this.snackBar.open('Membro removido.', 'OK', { duration: 3000 });
        this.loadMembers();
      },
    });
  }

  roleLabel(role: MemberRole): string {
    return role === 'EDITOR' ? 'Editor' : 'Visualizador';
  }

  close(): void {
    this.dialogRef.close();
  }
}
