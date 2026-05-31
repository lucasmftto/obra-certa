import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BrlMaskDirective } from '@core/pipes/brl-mask.directive';
import { ItemService } from '@core/services/item.service';
import { ItemType } from '@core/models/projeto.model';

@Component({
  selector: 'app-item-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatIconModule, MatCardModule, MatSnackBarModule, BrlMaskDirective,
  ],
  templateUrl: './item-form.component.html',
  styleUrls: ['./item-form.component.scss'],
})
export class ItemFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private itemService = inject(ItemService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  projectId = signal(0);
  environmentId = signal(0);
  itemId = signal<number | null>(null);
  salvando = signal(false);

  isEditing = computed(() => this.itemId() !== null);
  titulo = computed(() => this.isEditing() ? 'Editar item' : 'Novo item');

  units = ['un', 'm', 'm²', 'm³', 'kg', 'L', 'box', 'pc', 'hr', 'day'];

  form = this.fb.group({
    description: ['', [Validators.required, Validators.maxLength(300)]],
    quantity: [null as number | null, [Validators.required, Validators.min(0.001)]],
    unit: ['un', Validators.required],
    unitPrice: [null as number | null, [Validators.required, Validators.min(0)]],
    type: [null as ItemType | null, Validators.required],
  });

  ngOnInit() {
    this.projectId.set(+this.route.snapshot.paramMap.get('id')!);
    this.environmentId.set(+this.route.snapshot.paramMap.get('envId')!);
    const itemIdParam = this.route.snapshot.paramMap.get('itemId');
    if (itemIdParam) {
      this.itemId.set(+itemIdParam);
      this.itemService.findById(+itemIdParam).subscribe(item => {
        this.form.patchValue({
          description: item.description,
          quantity: item.quantity,
          unit: item.unit,
          unitPrice: item.unitPrice,
          type: item.type,
        });
      });
    }
  }

  salvar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true);
    const dados = this.form.getRawValue() as any;

    const op = this.isEditing()
      ? this.itemService.update(this.itemId()!, dados)
      : this.itemService.create(this.environmentId(), dados);

    op.subscribe({
      next: () => {
        this.snackBar.open(this.isEditing() ? 'Item atualizado!' : 'Item criado!', 'OK', { duration: 3000 });
        this.router.navigate(['/projects', this.projectId(), 'environments', this.environmentId()]);
      },
      error: () => this.salvando.set(false),
    });
  }

  voltar() {
    this.router.navigate(['/projects', this.projectId(), 'environments', this.environmentId()]);
  }
}
