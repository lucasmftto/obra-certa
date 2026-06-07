package com.obracerta.member.service;

import com.obracerta.auth.domain.User;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.member.domain.MemberRole;
import com.obracerta.member.domain.ProjectMember;
import com.obracerta.member.domain.ProjectMemberId;
import com.obracerta.member.dto.MemberRequest;
import com.obracerta.member.dto.MemberResponse;
import com.obracerta.member.repository.ProjectMemberRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.project.repository.ProjectRepository;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMemberService — Unit Tests")
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository memberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectMemberService memberService;

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_USER_ID = 2L;
    private static final Long PROJECT_ID = 10L;

    private User owner;
    private User memberUser;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = User.builder()
            .id(OWNER_ID).name("Owner").email("owner@example.com").password("hashed").build();

        memberUser = User.builder()
            .id(MEMBER_USER_ID).name("Member").email("member@example.com").password("hashed").build();

        project = Project.builder()
            .id(PROJECT_ID).name("Test Project").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_BUDGET)
            .owner(owner)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }

    // ------------------------------------------------------------------ //
    //  list                                                                //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("list — should return members when user is owner")
    void list_shouldReturnMembers_whenUserIsOwner() {
        ProjectMember member = ProjectMember.builder()
            .id(new ProjectMemberId(PROJECT_ID, MEMBER_USER_ID))
            .project(project).user(memberUser).role(MemberRole.EDITOR)
            .build();

        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        when(memberRepository.existsByIdProjectIdAndIdUserId(PROJECT_ID, OWNER_ID)).thenReturn(false);
        when(memberRepository.findByIdProjectId(PROJECT_ID)).thenReturn(List.of(member));

        List<MemberResponse> result = memberService.list(PROJECT_ID, OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("member@example.com");
        assertThat(result.get(0).role()).isEqualTo(MemberRole.EDITOR);
    }

    @Test
    @DisplayName("list — should throw 403 when user has no access")
    void list_shouldThrow403_whenUserHasNoAccess() {
        Long outsiderId = 99L;
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        when(memberRepository.existsByIdProjectIdAndIdUserId(PROJECT_ID, outsiderId)).thenReturn(false);

        assertThatThrownBy(() -> memberService.list(PROJECT_ID, outsiderId))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ------------------------------------------------------------------ //
    //  add                                                                 //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("add — should add member successfully when user is owner")
    void add_shouldAddMember_whenUserIsOwner() {
        MemberRequest request = new MemberRequest("member@example.com", MemberRole.VIEWER);

        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(memberUser));
        when(memberRepository.existsByIdProjectIdAndIdUserId(PROJECT_ID, MEMBER_USER_ID)).thenReturn(false);
        when(memberRepository.save(any(ProjectMember.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.add(PROJECT_ID, request, OWNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("member@example.com");
        assertThat(response.role()).isEqualTo(MemberRole.VIEWER);
        verify(memberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("add — should throw 403 when user is not owner")
    void add_shouldThrow403_whenUserIsNotOwner() {
        MemberRequest request = new MemberRequest("member@example.com", MemberRole.VIEWER);
        Long nonOwnerId = 5L;

        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> memberService.add(PROJECT_ID, request, nonOwnerId))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("add — should throw 409 when user is already a member")
    void add_shouldThrow409_whenAlreadyMember() {
        MemberRequest request = new MemberRequest("member@example.com", MemberRole.EDITOR);

        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(memberUser));
        when(memberRepository.existsByIdProjectIdAndIdUserId(PROJECT_ID, MEMBER_USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> memberService.add(PROJECT_ID, request, OWNER_ID))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ------------------------------------------------------------------ //
    //  remove                                                              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("remove — should remove member successfully when user is owner")
    void remove_shouldRemoveMember_whenUserIsOwner() {
        ProjectMember member = ProjectMember.builder()
            .id(new ProjectMemberId(PROJECT_ID, MEMBER_USER_ID))
            .project(project).user(memberUser).role(MemberRole.VIEWER)
            .build();

        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        when(memberRepository.findByIdProjectIdAndIdUserId(PROJECT_ID, MEMBER_USER_ID)).thenReturn(Optional.of(member));

        memberService.remove(PROJECT_ID, MEMBER_USER_ID, OWNER_ID);

        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("remove — should throw 403 when user is not owner")
    void remove_shouldThrow403_whenUserIsNotOwner() {
        Long nonOwnerId = 5L;
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> memberService.remove(PROJECT_ID, MEMBER_USER_ID, nonOwnerId))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(memberRepository, never()).delete(any());
    }
}
