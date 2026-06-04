import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { EnvironmentService } from '@core/services/environment.service';

@Component({
  selector: 'app-environment-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatButtonModule, MatFormFieldModule, MatInputModule,
    MatIconModule, MatCardModule, MatSnackBarModule,
  ],
  templateUrl: './environment-form.component.html',
  styleUrls: ['./environment-form.component.scss'],
})
export class EnvironmentFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private environmentService = inject(EnvironmentService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  projectId = signal(0);
  envId = signal<number | null>(null);
  salvando = signal(false);

  isEditing = computed(() => this.envId() !== null);
  titulo = computed(() => this.isEditing() ? 'Editar ambiente' : 'Novo ambiente');

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', Validators.maxLength(500)],
  });

  ngOnInit() {
    this.projectId.set(+this.route.snapshot.paramMap.get('id')!);
    const envIdParam = this.route.snapshot.paramMap.get('envId');
    if (envIdParam) {
      this.envId.set(+envIdParam);
      this.environmentService.findById(+envIdParam).subscribe(env => {
        this.form.patchValue({
          name: env.name,
          description: env.description ?? '',
        });
      });
    }
  }

  salvar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true);
    const dados = this.form.getRawValue() as any;

    const op = this.isEditing()
      ? this.environmentService.update(this.envId()!, dados)
      : this.environmentService.create(this.projectId(), dados);

    op.subscribe({
      next: () => {
        this.snackBar.open(
          this.isEditing() ? 'Ambiente atualizado!' : 'Ambiente criado!',
          'OK',
          { duration: 3000 }
        );
        this.router.navigate(['/projects', this.projectId()]);
      },
      error: () => this.salvando.set(false),
    });
  }
}
