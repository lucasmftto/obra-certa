package com.obracerta.member.repository;

import com.obracerta.member.domain.ProjectMember;
import com.obracerta.member.domain.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    List<ProjectMember> findByIdProjectId(Long projectId);

    Optional<ProjectMember> findByIdProjectIdAndIdUserId(Long projectId, Long userId);

    boolean existsByIdProjectIdAndIdUserId(Long projectId, Long userId);
}
