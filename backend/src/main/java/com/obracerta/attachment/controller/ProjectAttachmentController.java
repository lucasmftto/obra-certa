package com.obracerta.attachment.controller;

import com.obracerta.attachment.domain.ProjectAttachment;
import com.obracerta.attachment.dto.AttachmentResponse;
import com.obracerta.attachment.service.ProjectAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/attachments")
public class ProjectAttachmentController {

    private final ProjectAttachmentService service;

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(
        @PathVariable Long projectId,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(projectId, file));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> getContent(
        @PathVariable Long projectId,
        @PathVariable Long id
    ) {
        ProjectAttachment attachment = service.getContent(id);

        boolean isInline = attachment.getContentType().startsWith("image/")
            || attachment.getContentType().equals("application/pdf");

        ContentDisposition contentDisposition = isInline
            ? ContentDisposition.inline().filename(attachment.getFileName()).build()
            : ContentDisposition.attachment().filename(attachment.getFileName()).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(attachment.getContentType()));
        headers.setContentDisposition(contentDisposition);

        return ResponseEntity.ok().headers(headers).body(attachment.getContent());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long projectId,
        @PathVariable Long id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
