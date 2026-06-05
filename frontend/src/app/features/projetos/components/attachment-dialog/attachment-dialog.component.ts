import {
  Component,
  OnInit,
  inject,
  signal,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import * as XLSX from 'xlsx';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AttachmentService } from '@core/services/attachment.service';
import { Attachment } from '@core/models/projeto.model';

export interface AttachmentDialogData {
  projectId: number;
}

type ViewState = 'list' | 'preview';

@Component({
  selector: 'app-attachment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule,
  ],
  templateUrl: './attachment-dialog.component.html',
  styleUrls: ['./attachment-dialog.component.scss'],
})
export class AttachmentDialogComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  @ViewChild('pdfFrame') set pdfFrame(ref: ElementRef<HTMLIFrameElement> | undefined) {
    if (ref && this.currentBlobUrl) {
      ref.nativeElement.src = this.currentBlobUrl;
    }
  }

  private dialogRef = inject(MatDialogRef<AttachmentDialogComponent>);
  private data: AttachmentDialogData = inject(MAT_DIALOG_DATA);
  private attachmentService = inject(AttachmentService);
  private sanitizer = inject(DomSanitizer);
  private snackBar = inject(MatSnackBar);

  readonly projectId = this.data.projectId;

  attachments = signal<Attachment[]>([]);
  loading = signal(true);
  uploading = signal(false);
  loadingPreview = signal(false);

  viewState = signal<ViewState>('list');
  selectedAttachment = signal<Attachment | null>(null);
  previewImageUrl = signal<string | null>(null);
  previewExcelHtml = signal<SafeHtml | null>(null);
  private currentBlobUrl: string | null = null;

  ngOnInit(): void {
    this.loadAttachments();
  }

  loadAttachments(): void {
    this.loading.set(true);
    this.attachmentService.list(this.projectId).subscribe({
      next: (lista) => {
        this.attachments.set(lista);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openFilePicker(): void {
    this.fileInput.nativeElement.click();
  }

  uploadFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploading.set(true);
    this.attachmentService.upload(this.projectId, file).subscribe({
      next: () => {
        this.uploading.set(false);
        input.value = '';
        this.snackBar.open('Arquivo anexado!', 'OK', { duration: 3000 });
        this.loadAttachments();
      },
      error: () => {
        this.uploading.set(false);
        input.value = '';
      },
    });
  }

  preview(attachment: Attachment): void {
    this.selectedAttachment.set(attachment);
    this.viewState.set('preview');
    this.loadingPreview.set(true);

    if (this.isPdf(attachment.contentType) || this.isExcel(attachment.contentType)) {
      this.dialogRef.updateSize('90vw', '90vh');
    }

    if (this.isExcel(attachment.contentType)) {
      this.attachmentService.fetchArrayBuffer(this.projectId, attachment.id).subscribe({
        next: (buffer) => {
          const workbook = XLSX.read(buffer, { type: 'array' });
          const sheetName = workbook.SheetNames[0];
          const html = XLSX.utils.sheet_to_html(workbook.Sheets[sheetName]);
          this.previewExcelHtml.set(this.sanitizer.bypassSecurityTrustHtml(html));
          this.loadingPreview.set(false);
        },
        error: () => this.loadingPreview.set(false),
      });
      return;
    }

    this.attachmentService.fetchBlob(this.projectId, attachment.id).subscribe({
      next: (blobUrl) => {
        this.revokeCurrent();
        this.currentBlobUrl = blobUrl;
        if (this.isImage(attachment.contentType)) {
          this.previewImageUrl.set(blobUrl);
        }
        this.loadingPreview.set(false);
      },
      error: () => this.loadingPreview.set(false),
    });
  }

  backToList(): void {
    this.revokeCurrent();
    this.dialogRef.updateSize('560px', '');
    this.viewState.set('list');
    this.previewImageUrl.set(null);
    this.previewExcelHtml.set(null);
    this.selectedAttachment.set(null);
  }

  private revokeCurrent(): void {
    if (this.currentBlobUrl) {
      URL.revokeObjectURL(this.currentBlobUrl);
      this.currentBlobUrl = null;
    }
  }

  deleteAttachment(attachment: Attachment): void {
    if (!confirm(`Remover "${attachment.fileName}"?`)) return;
    this.attachmentService.delete(this.projectId, attachment.id).subscribe({
      next: () => {
        this.snackBar.open('Anexo removido.', 'OK', { duration: 3000 });
        if (this.selectedAttachment()?.id === attachment.id) this.backToList();
        this.loadAttachments();
      },
    });
  }

  close(): void {
    this.revokeCurrent();
    this.previewImageUrl.set(null);
    this.previewExcelHtml.set(null);
    this.dialogRef.close(this.attachments().length);
  }

  isImage(contentType: string): boolean {
    return contentType.startsWith('image/');
  }

  isPdf(contentType: string): boolean {
    return contentType === 'application/pdf';
  }

  isExcel(contentType: string): boolean {
    return contentType === 'application/vnd.ms-excel' ||
      contentType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
  }

  canPreview(contentType: string): boolean {
    return this.isImage(contentType) || this.isPdf(contentType) || this.isExcel(contentType);
  }

  getIcon(contentType: string): string {
    if (this.isImage(contentType)) return 'image';
    if (this.isPdf(contentType)) return 'picture_as_pdf';
    return 'table_chart';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  download(attachment: Attachment): void {
    const url = this.attachmentService.getContentUrl(this.projectId, attachment.id);
    const a = document.createElement('a');
    a.href = url;
    a.download = attachment.fileName;
    a.click();
  }
}
