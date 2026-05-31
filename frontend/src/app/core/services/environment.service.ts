import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Environment, EnvironmentRequest } from '@core/models/projeto.model';

@Injectable({ providedIn: 'root' })
export class EnvironmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listByProject(projectId: number): Observable<Environment[]> {
    return this.http.get<Environment[]>(`${this.apiUrl}/projects/${projectId}/environments`);
  }

  findById(id: number): Observable<Environment> {
    return this.http.get<Environment>(`${this.apiUrl}/environments/${id}`);
  }

  create(projectId: number, data: EnvironmentRequest): Observable<Environment> {
    return this.http.post<Environment>(`${this.apiUrl}/projects/${projectId}/environments`, data);
  }

  update(id: number, data: EnvironmentRequest): Observable<Environment> {
    return this.http.put<Environment>(`${this.apiUrl}/environments/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/environments/${id}`);
  }
}
