import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { MemberRequest, MemberResponse } from '@core/models/member.model';

@Injectable({ providedIn: 'root' })
export class MemberService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  list(projectId: number): Observable<MemberResponse[]> {
    return this.http.get<MemberResponse[]>(`${this.apiUrl}/projects/${projectId}/members`);
  }

  add(projectId: number, request: MemberRequest): Observable<MemberResponse> {
    return this.http.post<MemberResponse>(`${this.apiUrl}/projects/${projectId}/members`, request);
  }

  remove(projectId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/projects/${projectId}/members/${userId}`);
  }
}
