package com.obracerta.member.controller;

import com.obracerta.auth.domain.User;
import com.obracerta.member.dto.MemberRequest;
import com.obracerta.member.dto.MemberResponse;
import com.obracerta.member.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> list(
        @PathVariable Long projectId,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(memberService.list(projectId, currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> add(
        @PathVariable Long projectId,
        @Valid @RequestBody MemberRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.add(projectId, request, currentUser.getId()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(
        @PathVariable Long projectId,
        @PathVariable Long userId,
        @AuthenticationPrincipal User currentUser
    ) {
        memberService.remove(projectId, userId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
