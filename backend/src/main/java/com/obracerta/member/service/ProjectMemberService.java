package com.obracerta.member.service;

import com.obracerta.auth.domain.User;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.member.domain.ProjectMember;
import com.obracerta.member.domain.ProjectMemberId;
import com.obracerta.member.dto.MemberRequest;
import com.obracerta.member.dto.MemberResponse;
import com.obracerta.member.repository.ProjectMemberRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.repository.ProjectRepository;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MemberResponse> list(Long projectId, Long currentUserId) {
        Project project = findProject(projectId);
        checkAccess(project, currentUserId);
        return memberRepository.findByIdProjectId(projectId).stream()
            .map(m -> new MemberResponse(
                m.getUser().getId(),
                m.getUser().getName(),
                m.getUser().getEmail(),
                m.getRole()
            ))
            .toList();
    }

    @Transactional
    public MemberResponse add(Long projectId, MemberRequest request, Long currentUserId) {
        Project project = findProject(projectId);
        checkOwner(project, currentUserId);

        User targetUser = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com e-mail: " + request.email()));

        if (targetUser.getId().equals(project.getOwner().getId())) {
            throw new BusinessException("O owner do projeto não pode ser adicionado como membro.", HttpStatus.CONFLICT);
        }

        if (memberRepository.existsByIdProjectIdAndIdUserId(projectId, targetUser.getId())) {
            throw new BusinessException("Usuário já é membro deste projeto.", HttpStatus.CONFLICT);
        }

        ProjectMember member = ProjectMember.builder()
            .id(new ProjectMemberId(projectId, targetUser.getId()))
            .project(project)
            .user(targetUser)
            .role(request.role())
            .build();

        memberRepository.save(member);
        return new MemberResponse(targetUser.getId(), targetUser.getName(), targetUser.getEmail(), member.getRole());
    }

    @Transactional
    public void remove(Long projectId, Long targetUserId, Long currentUserId) {
        Project project = findProject(projectId);
        checkOwner(project, currentUserId);

        ProjectMember member = memberRepository.findByIdProjectIdAndIdUserId(projectId, targetUserId)
            .orElseThrow(() -> new EntityNotFoundException("Membro não encontrado no projeto."));

        memberRepository.delete(member);
    }

    // ---- helpers ----

    private Project findProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + projectId));
    }

    private void checkAccess(Project project, Long userId) {
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(userId);
        boolean isMember = memberRepository.existsByIdProjectIdAndIdUserId(project.getId(), userId);
        if (!isOwner && !isMember) {
            throw new BusinessException("Acesso negado ao projeto.", HttpStatus.FORBIDDEN);
        }
    }

    private void checkOwner(Project project, Long userId) {
        if (project.getOwner() == null || !project.getOwner().getId().equals(userId)) {
            throw new BusinessException("Apenas o owner pode gerenciar membros do projeto.", HttpStatus.FORBIDDEN);
        }
    }
}
