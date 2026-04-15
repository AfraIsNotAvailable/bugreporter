package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.BugRequest;
import com.group11.bugreporter.entity.*;
import com.group11.bugreporter.entity.enums.BugStatus;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BugServiceTest {

    @Mock private BugRepository bugRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;

    @InjectMocks private BugService bugService;

    @Test
    void testCreateBug_InitialStatusIs_OPEN() {
        // intial status - opem
        User author = new User(); author.setId(1L);
        BugRequest request = new BugRequest();
        request.setTitle("Titlu"); request.setText("Text");

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bugRepository.save(any(Bug.class))).thenAnswer(i -> i.getArgument(0));

        Bug result = bugService.createBug(request, 1L);

        assertEquals(BugStatus.OPEN, result.getStatus());
        verify(bugRepository).save(any(Bug.class));
    }

    @Test
    void testDeleteBug_OnlyAuthorCanDelete() {
        // restrictioneaza stergerea doar pt autor
        User author = new User(); author.setId(1L);
        User stranger = new User(); stranger.setId(2L);
        Bug bug = Bug.builder().id(10L).author(author).build();

        when(bugRepository.findById(10L)).thenReturn(Optional.of(bug));

        // Un strain nu are voie
        assertThrows(ForbiddenException.class, () -> bugService.deleteBug(10L, 2L, Role.USER));

        // Autorul are voie
        assertDoesNotThrow(() -> bugService.deleteBug(10L, 1L, Role.USER));

        // Admin-ul are voie indiferent de autor
        assertDoesNotThrow(() -> bugService.deleteBug(10L, 2L, Role.ADMIN));

        // Moderatorul are voie indiferent de autor
        assertDoesNotThrow(() -> bugService.deleteBug(10L, 2L, Role.MODERATOR));
    }

    @Test
    void testAddTags_ShouldCreateNewTagIfMissing() {
        // permite crearea unei etichete noi daca nu exista
        Bug bug = Bug.builder().id(1L).tags(new HashSet<>()).build();
        when(bugRepository.findById(1L)).thenReturn(Optional.of(bug));
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(i -> i.getArgument(0));
        when(bugRepository.save(any(Bug.class))).thenAnswer(i -> i.getArgument(0));

        bugService.addTagsToBug(1L, List.of("Java"));

        verify(tagRepository).save(any(Tag.class)); // S-a creat tag-ul nou
        assertEquals(1, bug.getTags().size());
    }
}