# Obra Certa — Spec Funcional: Módulo Projetos

## Visão geral

O módulo de Projetos é o ponto de entrada do sistema. Todo o restante (ambientes, itens, gastos) existe dentro de um projeto. O usuário cria um projeto para cada obra que deseja orçar e acompanhar.

---

## Entidade: Projeto

Representa uma obra de reforma. Pode ser uma casa, apartamento, ou outro imóvel.

---

## Status do projeto

O projeto passa pelos seguintes estados:

```
EM_ORCAMENTO → EM_ANDAMENTO → CONCLUIDO
                     ↓
                 PAUSADO → EM_ANDAMENTO
```

| Status | Descrição |
|---|---|
| `EM_ORCAMENTO` | Projeto criado, orçamento ainda sendo montado. Nenhum gasto lançado. |
| `EM_ANDAMENTO` | Obra iniciada. Gastos podem ser lançados. |
| `PAUSADO` | Obra temporariamente interrompida. Gastos não podem ser lançados. |
| `CONCLUIDO` | Obra finalizada. Nenhuma alteração permitida. |

---

## Regras de negócio

### RN-PROJ-01 — Criação
- Todo projeto deve ter pelo menos nome e tipo (CASA ou APARTAMENTO).
- O status inicial é sempre `EM_ORCAMENTO`.
- A data de criação é preenchida automaticamente pelo sistema.

### RN-PROJ-02 — Edição
- Nome, endereço, tipo e descrição podem ser editados em qualquer status, exceto `CONCLUIDO`.
- O status `CONCLUIDO` bloqueia qualquer edição no projeto e em seus filhos (ambientes, itens, gastos).

### RN-PROJ-03 — Transição de status
- Somente as transições definidas no diagrama acima são permitidas.
- Para avançar de `EM_ORCAMENTO` para `EM_ANDAMENTO`, o projeto deve ter ao menos um ambiente com ao menos um item orçado.
- Não é permitido voltar de `CONCLUIDO` para qualquer outro status.

### RN-PROJ-04 — Exclusão
- Um projeto só pode ser excluído se estiver com status `EM_ORCAMENTO`.
- A exclusão é lógica (soft delete via campo `excluido_em`).
- A exclusão de um projeto cascateia para todos os seus ambientes, itens e gastos.

### RN-PROJ-05 — Cálculos
O sistema deve calcular e expor em tempo real:
- **Total orçado:** soma de `(quantidade × valor_unitario)` de todos os itens do projeto. Representa o teto macro do orçamento.
- **Total previsto:** soma de `valor_previsto` de todos os gastos do projeto. Representa o comprometimento operacional planejado.
- **Total efetivo:** soma de `valor_efetivo` dos gastos confirmados (onde `valor_efetivo IS NOT NULL`). Representa o que foi efetivamente pago.
- **Saldo restante:** total orçado − total efetivo.
- **Percentual efetivo:** (total efetivo / total orçado) × 100.
- **Percentual concluído:** média dos `percentual_concluido` de todos os ambientes.

### RN-PROJ-06 — Alerta de estouro
- Se `total efetivo > total orçado`, o projeto entra em estado de alerta visual.
- O sistema não bloqueia o lançamento de gastos acima do orçado, apenas sinaliza.

---

## Fluxos principais

### Fluxo 1 — Criar projeto
1. Usuário acessa a listagem de projetos.
2. Clica em "Novo projeto".
3. Preenche nome, tipo, endereço (opcional) e descrição (opcional).
4. Confirma. Sistema cria o projeto com status `EM_ORCAMENTO`.
5. Usuário é redirecionado para a tela de detalhes do projeto.

### Fluxo 2 — Iniciar obra
1. Usuário acessa os detalhes do projeto com status `EM_ORCAMENTO`.
2. Clica em "Iniciar obra".
3. Sistema valida que há ao menos um ambiente com um item.
4. Status muda para `EM_ANDAMENTO`.

### Fluxo 3 — Concluir obra
1. Usuário acessa os detalhes do projeto com status `EM_ANDAMENTO` ou `PAUSADO`.
2. Clica em "Concluir obra".
3. Sistema exibe confirmação com resumo financeiro (total orçado, total gasto, saldo).
4. Usuário confirma. Status muda para `CONCLUIDO`.

### Fluxo 4 — Excluir projeto
1. Usuário acessa a listagem ou detalhes do projeto com status `EM_ORCAMENTO`.
2. Clica em "Excluir".
3. Sistema exibe confirmação alertando que todos os dados serão removidos.
4. Usuário confirma. Soft delete aplicado.

---

## Validações

| Campo | Regra |
|---|---|
| `nome` | Obrigatório. Mínimo 3 caracteres. Máximo 200 caracteres. |
| `tipo` | Obrigatório. Valores aceitos: `CASA`, `APARTAMENTO`. |
| `endereco` | Opcional. Máximo 300 caracteres. |
| `descricao` | Opcional. Máximo 500 caracteres. |
| `status` | Gerenciado pelo sistema. Não enviado pelo cliente na criação. |
