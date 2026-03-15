package com.group11.bugreporter.controller;

import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.VoteType;
import com.group11.bugreporter.exception.InvalidVoteTypeException;
import com.group11.bugreporter.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    @Test
    void voteComment_throwsBadRequestForInvalidVoteType() {
        InvalidVoteTypeException ex = assertThrows(
                InvalidVoteTypeException.class,
                () -> commentController.voteComment(1L, 2L, "sidevote")
        );

        assertEquals("Invalid voteType 'sidevote'. Allowed values: UPVOTE, DOWNVOTE.", ex.getMessage());
    }

    @Test
    void voteComment_throwsBadRequestForNullVoteType() {
        InvalidVoteTypeException ex = assertThrows(
                InvalidVoteTypeException.class,
                () -> commentController.voteComment(1L, 2L, null)
        );

        assertEquals("voteType is required. Allowed values: UPVOTE, DOWNVOTE.", ex.getMessage());
    }

    @Test
    void voteComment_throwsBadRequestForBlankVoteType() {
        InvalidVoteTypeException ex = assertThrows(
                InvalidVoteTypeException.class,
                () -> commentController.voteComment(1L, 2L, "   ")
        );

        assertEquals("voteType is required. Allowed values: UPVOTE, DOWNVOTE.", ex.getMessage());
    }

    @Test
    void voteComment_acceptsCaseInsensitiveVoteType() {
        User author = new User();
        author.setId(3L);
        Comment comment = Comment.builder().id(1L).score(1).text("ok").author(author).build();

        when(commentService.voteComment(1L, 2L, VoteType.UPVOTE)).thenReturn(comment);

        assertEquals(HttpStatus.OK, commentController.voteComment(1L, 2L, "upvote").getStatusCode());
    }
}


