import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  Project,
  ProjectRequest,
  ProjectSummary,
  PageResponse,
  ProjectStatus,
  StatusRequest,
  ProjectFilters,
} from '@core/models/projeto.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/projects`;

  list(filters: ProjectFilters = {}): Observable<PageResponse<Project>> {
    let params = new HttpParams();
    if (filters.status) params = params.set('status', filters.status);
    if (filters.type)   params = params.set('type', filters.type);
    if (filters.search) params = params.set('search', filters.search);
    if (filters.page != null) params = params.set('page', filters.page);
    if (filters.size != null) params = params.set('size', filters.size);
    if (filters.sort)   params = params.set('sort', filters.sort);
    return this.http.get<PageResponse<Project>>(this.baseUrl, { params });
  }

  findById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/${id}`);
  }

  create(data: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, data);
  }

  update(id: number, data: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/${id}`, data);
  }

  updateStatus(id: number, status: ProjectStatus): Observable<Project> {
    const body: StatusRequest = { status };
    return this.http.patch<Project>(`${this.baseUrl}/${id}/status`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  summary(id: number): Observable<ProjectSummary> {
    return this.http.get<ProjectSummary>(`${this.baseUrl}/${id}/resumo`);
  }
}
