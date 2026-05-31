import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Item, ItemFlagsRequest, ItemRequest } from '@core/models/projeto.model';

@Injectable({ providedIn: 'root' })
export class ItemService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listByEnvironment(environmentId: number): Observable<Item[]> {
    return this.http.get<Item[]>(`${this.apiUrl}/environments/${environmentId}/items`);
  }

  findById(id: number): Observable<Item> {
    return this.http.get<Item>(`${this.apiUrl}/items/${id}`);
  }

  create(environmentId: number, data: ItemRequest): Observable<Item> {
    return this.http.post<Item>(`${this.apiUrl}/environments/${environmentId}/items`, data);
  }

  update(id: number, data: ItemRequest): Observable<Item> {
    return this.http.put<Item>(`${this.apiUrl}/items/${id}`, data);
  }

  updateFlags(id: number, flags: ItemFlagsRequest): Observable<Item> {
    return this.http.patch<Item>(`${this.apiUrl}/items/${id}/flags`, flags);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${id}`);
  }
}
