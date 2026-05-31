import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Expense, ExpenseRequest } from '@core/models/projeto.model';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listByItem(itemId: number): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.apiUrl}/items/${itemId}/expenses`);
  }

  findById(id: number): Observable<Expense> {
    return this.http.get<Expense>(`${this.apiUrl}/expenses/${id}`);
  }

  create(itemId: number, data: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>(`${this.apiUrl}/items/${itemId}/expenses`, data);
  }

  update(id: number, data: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`${this.apiUrl}/expenses/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/expenses/${id}`);
  }
}
