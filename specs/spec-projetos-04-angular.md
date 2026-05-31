# Obra Certa — Spec Angular: Módulo Projetos

## Estrutura de arquivos

```
src/app/features/projetos/
├── projetos.module.ts
├── projetos-routing.module.ts
├── pages/
│   ├── projetos-lista/
│   │   ├── projetos-lista.component.ts
│   │   ├── projetos-lista.component.html
│   │   └── projetos-lista.component.scss
│   ├── projeto-detalhe/
│   │   ├── projeto-detalhe.component.ts
│   │   ├── projeto-detalhe.component.html
│   │   └── projeto-detalhe.component.scss
│   └── projeto-form/
│       ├── projeto-form.component.ts
│       ├── projeto-form.component.html
│       └── projeto-form.component.scss
├── components/
│   ├── projeto-card/
│   │   ├── projeto-card.component.ts
│   │   └── projeto-card.component.html
│   └── projeto-status-badge/
│       ├── projeto-status-badge.component.ts
│       └── projeto-status-badge.component.html
└── services/
    └── projeto.service.ts
```

---

## Rotas

```typescript
// projetos-routing.module.ts
const routes: Routes = [
  { path: '',           component: ProjetosListaComponent },
  { path: 'novo',       component: ProjetoFormComponent },
  { path: ':id',        component: ProjetoDetalheComponent },
  { path: ':id/editar', component: ProjetoFormComponent },
];
```

---

## Models

```typescript
// src/app/core/models/projeto.model.ts

export type TipoProjeto = 'CASA' | 'APARTAMENTO';

export type StatusProjeto =
  | 'EM_ORCAMENTO'
  | 'EM_ANDAMENTO'
  | 'PAUSADO'
  | 'CONCLUIDO';

export interface Projeto {
  id: number;
  nome: string;
  tipo: TipoProjeto;
  endereco?: string;
  descricao?: string;
  status: StatusProjeto;
  totalOrcado: number;
  totalPrevisto: number;
  totalEfetivo: number;
  saldoRestante: number;
  percentualEfetivo: number;
  percentualConcluido: number;
  emAlerta: boolean;
  totalAmbientes: number;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ProjetoRequest {
  nome: string;
  tipo: TipoProjeto;
  endereco?: string;
  descricao?: string;
}

export interface ProjetoResumo {
  projetoId: number;
  nome: string;
  status: StatusProjeto;
  totais: {
    orcado: number;
    previsto: number;
    efetivo: number;
    saldo: number;
    percentualEfetivo: number;
    percentualConcluido: number;
    emAlerta: boolean;
  };
  porAmbiente: {
    ambienteId: number;
    nome: string;
    orcado: number;
    previsto: number;
    efetivo: number;
    saldo: number;
    percentualEfetivo: number;
    percentualConcluido: number;
    emAlerta: boolean;
  }[];
  ultimosGastos: {
    gastoId: number;
    descricao: string;
    valorPrevisto: number;
    valorEfetivo: number | null;
    dataPrevista: string;
    dataEfetiva: string | null;
    pendente: boolean;
    ambienteNome: string;
  }[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

---

## Service

```typescript
// projeto.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Projeto, ProjetoRequest, ProjetoResumo, PageResponse, StatusProjeto, TipoProjeto } from '@core/models/projeto.model';

export interface ProjetoFiltros {
  status?: StatusProjeto;
  tipo?: TipoProjeto;
  busca?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProjetoService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/projetos`;

  listar(filtros: ProjetoFiltros = {}): Observable<PageResponse<Projeto>> {
    let params = new HttpParams();
    if (filtros.status)  params = params.set('status',  filtros.status);
    if (filtros.tipo)    params = params.set('tipo',    filtros.tipo);
    if (filtros.busca)   params = params.set('busca',   filtros.busca);
    if (filtros.page != null) params = params.set('page', filtros.page);
    if (filtros.size != null) params = params.set('size', filtros.size);
    if (filtros.sort)    params = params.set('sort',    filtros.sort);
    return this.http.get<PageResponse<Projeto>>(this.baseUrl, { params });
  }

  buscarPorId(id: number): Observable<Projeto> {
    return this.http.get<Projeto>(`${this.baseUrl}/${id}`);
  }

  criar(dados: ProjetoRequest): Observable<Projeto> {
    return this.http.post<Projeto>(this.baseUrl, dados);
  }

  atualizar(id: number, dados: ProjetoRequest): Observable<Projeto> {
    return this.http.put<Projeto>(`${this.baseUrl}/${id}`, dados);
  }

  atualizarStatus(id: number, status: StatusProjeto): Observable<Projeto> {
    return this.http.patch<Projeto>(`${this.baseUrl}/${id}/status`, { status });
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  resumo(id: number): Observable<ProjetoResumo> {
    return this.http.get<ProjetoResumo>(`${this.baseUrl}/${id}/resumo`);
  }
}
```

---

## Tela 1 — Lista de Projetos (`projetos-lista`)

### Comportamento
- Exibe cards de todos os projetos, paginados (20 por página).
- Filtros no topo: busca por texto, filtro de status, filtro de tipo.
- Botão "Novo projeto" no canto superior direito.
- Estado vazio: ilustração + mensagem "Nenhum projeto encontrado. Crie seu primeiro projeto."
- Estado de carregamento: skeleton loader nos cards.
- Ao clicar no card, navega para `/projetos/:id`.

### Campos exibidos no card (`projeto-card`)
- Nome do projeto
- Badge de status (componente `projeto-status-badge`)
- Tipo: ícone + label (casa / apartamento)
- Endereço (se preenchido)
- Barra de progresso: percentual concluído
- Valores: total orçado e total gasto
- Indicador de alerta (ícone vermelho) se `emAlerta = true`
- Total de ambientes

### Filtros
| Filtro | Componente Angular Material | Comportamento |
|---|---|---|
| Busca | `mat-form-field` + `matInput` | Debounce 400ms, busca ao digitar |
| Status | `mat-select` | Seleção única, opcional |
| Tipo | `mat-button-toggle-group` | Todos / Casa / Apartamento |

---

## Tela 2 — Formulário de Projeto (`projeto-form`)

### Comportamento
- Modo criação: título "Novo projeto", botão "Criar projeto".
- Modo edição: título "Editar projeto", pré-carrega dados, botão "Salvar alterações".
- Ao salvar com sucesso, redireciona para `/projetos/:id` com snackbar de confirmação.
- Ao cancelar, volta para a tela anterior.
- Campos desabilitados se projeto estiver com status `CONCLUIDO`.

### Formulário (Reactive Forms)

| Campo | Componente | Validação | Observações |
|---|---|---|---|
| Nome | `matInput` text | Obrigatório, min 3, max 200 | Foco automático ao abrir |
| Tipo | `mat-radio-group` | Obrigatório | Opções: Casa / Apartamento |
| Endereço | `matInput` text | Máx 300 | Opcional |
| Descrição | `matInput` textarea | Máx 500 | Opcional, 3 linhas |

### Mensagens de erro inline
- Nome vazio: "Nome é obrigatório"
- Nome curto: "Nome deve ter no mínimo 3 caracteres"
- Tipo não selecionado: "Selecione o tipo do imóvel"

---

## Tela 3 — Detalhe do Projeto (`projeto-detalhe`)

### Comportamento
- Cabeçalho com nome, badge de status e botões de ação contextuais.
- Aba ou seção de resumo financeiro (cards de totais).
- Listagem de ambientes do projeto (carregada do módulo de ambientes).
- Menu de ações: Editar, Mudar status, Excluir.

### Botões de ação por status

| Status atual | Ações disponíveis |
|---|---|
| `EM_ORCAMENTO` | Editar · Iniciar obra · Excluir |
| `EM_ANDAMENTO` | Editar · Pausar obra · Concluir obra |
| `PAUSADO` | Editar · Retomar obra · Concluir obra |
| `CONCLUIDO` | (somente visualização) |

### Cards de resumo financeiro
- Total orçado
- Total gasto
- Saldo restante (verde se positivo, vermelho se negativo)
- Percentual concluído (com `mat-progress-bar`)

### Confirmações obrigatórias
- "Excluir projeto": dialog com aviso de que todos os dados serão perdidos.
- "Concluir obra": dialog com resumo financeiro antes de confirmar.

---

## Componente: `projeto-status-badge`

Badge visual com cor por status:

| Status | Cor de fundo | Cor do texto | Label |
|---|---|---|---|
| `EM_ORCAMENTO` | Azul claro | Azul escuro | Em orçamento |
| `EM_ANDAMENTO` | Verde claro | Verde escuro | Em andamento |
| `PAUSADO` | Amarelo claro | Amarelo escuro | Pausado |
| `CONCLUIDO` | Cinza claro | Cinza escuro | Concluído |

```typescript
// projeto-status-badge.component.ts
@Component({
  selector: 'app-projeto-status-badge',
  template: `<span [class]="'badge badge--' + status.toLowerCase()">{{ label }}</span>`,
})
export class ProjetoStatusBadgeComponent {
  @Input({ required: true }) status!: StatusProjeto;

  get label(): string {
    const labels: Record<StatusProjeto, string> = {
      EM_ORCAMENTO: 'Em orçamento',
      EM_ANDAMENTO: 'Em andamento',
      PAUSADO: 'Pausado',
      CONCLUIDO: 'Concluído',
    };
    return labels[this.status];
  }
}
```

---

## Observações para o Claude Code

1. Usar **Signals** (`signal`, `computed`, `effect`) nos components em vez de `BehaviorSubject` onde possível.
2. Usar `inject()` no lugar de injeção por construtor.
3. Tratamento de erros centralizado via `HttpInterceptor` — exibir snackbar com a mensagem do backend.
4. O `ProjetoService` deve ser o único ponto de acesso à API — components não fazem chamadas HTTP diretamente.
5. Paginação com `MatPaginator` integrado ao endpoint paginado do backend.
6. Usar `trackBy` em todos os `*ngFor` para otimizar renderização.
7. Preferir **Standalone Components** (Angular 17+) ao padrão NgModule — refatorar o módulo `projetos` para standalone assim que os demais módulos forem criados.
