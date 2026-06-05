import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '@env/environment';
import { Attachment } from '@core/models/projeto.model';

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  list(projectId: number): Observable<Attachment[]> {
    return this.http.get<Attachment[]>(`${this.apiUrl}/projects/${projectId}/attachments`);
  }

  upload(projectId: number, file: File): Observable<Attachment> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Attachment>(`${this.apiUrl}/projects/${projectId}/attachments`, formData);
  }

  fetchBlob(projectId: number, attachmentId: number): Observable<string> {
    const url = `${this.apiUrl}/projects/${projectId}/attachments/${attachmentId}/content`;
    return this.http.get(url, { responseType: 'blob' }).pipe(
      map((blob) => URL.createObjectURL(blob))
    );
  }

  fetchArrayBuffer(projectId: number, attachmentId: number): Observable<ArrayBuffer> {
    const url = `${this.apiUrl}/projects/${projectId}/attachments/${attachmentId}/content`;
    return this.http.get(url, { responseType: 'arraybuffer' });
  }

  getContentUrl(projectId: number, attachmentId: number): string {
    return `${this.apiUrl}/projects/${projectId}/attachments/${attachmentId}/content`;
  }

  delete(projectId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/projects/${projectId}/attachments/${attachmentId}`);
  }
}
