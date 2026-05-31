import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProjectService } from '@core/services/projeto.service';
import { ProjectType } from '@core/models/projeto.model';

@Component({
  selector: 'app-projeto-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
  ],
  templateUrl: './projeto-form.component.html',
  styleUrls: ['./projeto-form.component.scss'],
})
export class ProjectFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private projectService = inject(ProjectService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  projetoId = signal<number | null>(null);
  salvando = signal(false);

  isEdicao = computed(() => this.projetoId() !== null);
  titulo = computed(() => (this.isEdicao() ? 'Editar projeto' : 'Novo projeto'));
  labelBotao = computed(() => (this.isEdicao() ? 'Salvar alterações' : 'Criar projeto'));

  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    type: [null as ProjectType | null, Validators.required],
    address: ['', Validators.maxLength(300)],
    description: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.projetoId.set(+id);
      this.projectService.findById(+id).subscribe((p) => {
        this.form.patchValue({
          name: p.name,
          type: p.type,
          address: p.address ?? '',
          description: p.description ?? '',
        });
        if (p.status === 'COMPLETED') this.form.disable();
      });
    }
  }

  voltar(): void {
    history.back();
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const dados = this.form.getRawValue() as {
      name: string;
      type: ProjectType;
      address: string;
      description: string;
    };

    const op = this.isEdicao()
      ? this.projectService.update(this.projetoId()!, dados)
      : this.projectService.create(dados);

    op.subscribe({
      next: (p) => {
        this.snackBar.open(
          this.isEdicao() ? 'Projeto atualizado!' : 'Projeto criado!',
          'OK',
          { duration: 3000 }
        );
        this.router.navigate(['/projects', p.id]);
      },
      error: () => this.salvando.set(false),
    });
  }
}
