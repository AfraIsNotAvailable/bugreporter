package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.request.BugRequest;
import com.group11.bugreporter.dto.response.BugResponse;
import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.BugStatus;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.service.BugService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BugControllerTest {

    @Mock private BugService bugService;
    @Mock private UserRepository userRepository;

    @InjectMocks private BugController bugController;

    private User testUser;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("catalin");
        auth = new TestingAuthenticationToken("catalin", "password", "ROLE_USER");
    }

    @Test
    void reportBug_ShouldReturnCreated() {
        BugRequest request = new BugRequest();
        request.setTitle("Bug Nou");

        Bug bug = Bug.builder()
                .id(10L)
                .title("Bug Nou")
                .status(BugStatus.OPEN)
                .author(testUser)
                .tags(Collections.emptySet())
                .build();

        when(userRepository.findByUsername("catalin")).thenReturn(Optional.of(testUser));
        when(bugService.createBug(any(BugRequest.class), eq(1L))).thenReturn(bug);

        ResponseEntity<BugResponse> response = bugController.reportBug(request, auth);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Bug Nou", response.getBody().getTitle());
    }

    @Test
    void filterBugs_Mine_ShouldReturnOk() {
        Bug bug = Bug.builder()
                .id(1L)
                .title("Test Mine")
                .status(BugStatus.OPEN)
                .author(testUser)
                .tags(Collections.emptySet())
                .build();

        when(userRepository.findByUsername("catalin")).thenReturn(Optional.of(testUser));
        when(bugService.getBugsByAuthor(1L)).thenReturn(List.of(bug));

        ResponseEntity<List<BugResponse>> response = bugController.filterBugs(null, null, null, true, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(bugService).getBugsByAuthor(1L);
    }

    @Test
    void resolveBug_ShouldReturnFixedStatus() {
        Bug resolvedBug = Bug.builder()
                .id(1L)
                .title("Fixed Bug")
                .status(BugStatus.FIXED)
                .author(testUser)
                .tags(Collections.emptySet())
                .build();

        when(userRepository.findByUsername("catalin")).thenReturn(Optional.of(testUser));
        when(bugService.resolveBug(eq(1L), eq(1L), any())).thenReturn(resolvedBug);

        ResponseEntity<BugResponse> response = bugController.resolveBug(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(BugStatus.FIXED.name(), response.getBody().getStatus());
    }
}