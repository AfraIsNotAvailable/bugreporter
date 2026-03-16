package com.group11.bugreporter.controller;

import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.entity.enums.VoteType;
import com.group11.bugreporter.dto.request.VoteRequest;
import com.group11.bugreporter.exception.InvalidVoteTypeException;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentController commentController;

    @Test
    void voteComment_throwsBadRequestForInvalidVoteType() {
        User voter = new User();
        voter.setId(2L);
        voter.setUsername("voter");
        voter.setRole(Role.USER);
        Authentication auth = new TestingAuthenticationToken("voter", "credentials", "ROLE_USER");
        when(userRepository.findByUsername("voter")).thenReturn(Optional.of(voter));

        VoteRequest payload = new VoteRequest();
        payload.setVoteType("sidevote");

        InvalidVoteTypeException ex = assertThrows(
                InvalidVoteTypeException.class,
                () -> commentController.voteComment(1L, auth, payload)
        );

        assertEquals("Invalid voteType 'sidevote'. Allowed values: UPVOTE, DOWNVOTE.", ex.getMessage());
    }

    @Test
    void voteComment_throwsBadRequestForNullVoteType() {
        User voter = new User();
        voter.setId(2L);
        voter.setUsername("voter");
        voter.setRole(Role.USER);
        Authentication auth = new TestingAuthenticationToken("voter", "credentials", "ROLE_USER");
        when(userRepository.findByUsername("voter")).thenReturn(Optional.of(voter));

        VoteRequest payload = new VoteRequest();

        InvalidVoteTypeException ex = assertThrows(
                InvalidVoteTypeException.class,
                () -> commentController.voteComment(1L, auth, payload)
        );

        assertEquals("voteType is required. Allowed values: UPVOTE, DOWNVOTE.", ex.getMessage());
    }

    @Test
    void voteComment_throwsBadRequestForBlankVoteType() {
        User voter = new User();
        voter.setId(2L);
        voter.setUsername("voter");
        voter.setRole(Role.USER);
        Authentication auth = new TestingAuthenticationToken("voter", "credentials", "ROLE_USER");
        when(userRepository.findByUsername("voter")).thenReturn(Optional.of(voter));

        VoteRequest payload = new VoteRequest();
        payload.setVoteType("   ");

        InvalidVoteTypeException ex = assertThrows(
                InvalidVoteTypeException.class,
                () -> commentController.voteComment(1L, auth, payload)
        );

        assertEquals("voteType is required. Allowed values: UPVOTE, DOWNVOTE.", ex.getMessage());
    }

    @Test
    void voteComment_acceptsCaseInsensitiveVoteType() {
        User author = new User();
        author.setId(3L);

        User voter = new User();
        voter.setId(2L);
        voter.setUsername("voter");
        voter.setRole(Role.USER);

        Authentication auth = new TestingAuthenticationToken("voter", "credentials", "ROLE_USER");
        when(userRepository.findByUsername("voter")).thenReturn(Optional.of(voter));

        VoteRequest payload = new VoteRequest();
        payload.setVoteType("upvote");

        Comment comment = Comment.builder().id(1L).score(1).text("ok").author(author).build();

        when(commentService.voteComment(1L, 2L, VoteType.UPVOTE)).thenReturn(comment);

        assertEquals(HttpStatus.OK, commentController.voteComment(1L, auth, payload).getStatusCode());
    }
}


