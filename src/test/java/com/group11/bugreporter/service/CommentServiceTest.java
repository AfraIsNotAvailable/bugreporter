package com.group11.bugreporter.service;

import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.CommentVote;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.VoteType;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.CommentRepository;
import com.group11.bugreporter.repository.CommentVoteRepository;
import com.group11.bugreporter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentVoteRepository commentVoteRepository;

    @Mock
    private BugRepository bugRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private static final Long COMMENT_ID = 10L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private Comment comment;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setId(AUTHOR_ID);

        comment = Comment.builder()
                .id(COMMENT_ID)
                .author(author)
                .score(0)
                .text("test")
                .build();
    }

    @Test
    void voteComment_createsNewUpvoteAndIncrementsScore() {
        User voter = new User();
        voter.setId(OTHER_USER_ID);

        when(commentRepository.findByIdForUpdate(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(voter));
        when(commentVoteRepository.findByCommentIdAndUserId(COMMENT_ID, OTHER_USER_ID)).thenReturn(Optional.empty());
        when(commentRepository.save(comment)).thenReturn(comment);

        Comment result = commentService.voteComment(COMMENT_ID, OTHER_USER_ID, VoteType.UPVOTE);

        assertEquals(1, result.getScore());
        verify(commentVoteRepository).save(any(CommentVote.class));
        verify(commentRepository).save(comment);
    }

    @Test
    void voteComment_sameVoteRemovesExistingVoteAndDecrementsScore() {
        User voter = new User();
        voter.setId(OTHER_USER_ID);

        comment.setScore(4);

        CommentVote existingVote = CommentVote.builder()
                .comment(comment)
                .user(voter)
                .voteType(VoteType.UPVOTE)
                .build();

        when(commentRepository.findByIdForUpdate(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(voter));
        when(commentVoteRepository.findByCommentIdAndUserId(COMMENT_ID, OTHER_USER_ID)).thenReturn(Optional.of(existingVote));
        when(commentRepository.save(comment)).thenReturn(comment);

        Comment result = commentService.voteComment(COMMENT_ID, OTHER_USER_ID, VoteType.UPVOTE);

        assertEquals(3, result.getScore());
        verify(commentVoteRepository).delete(existingVote);
        verify(commentRepository).save(comment);
    }

    @Test
    void voteComment_switchesVoteAndAdjustsScoreByTwo() {
        User voter = new User();
        voter.setId(OTHER_USER_ID);

        comment.setScore(-1);

        CommentVote existingVote = CommentVote.builder()
                .comment(comment)
                .user(voter)
                .voteType(VoteType.DOWNVOTE)
                .build();

        when(commentRepository.findByIdForUpdate(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(voter));
        when(commentVoteRepository.findByCommentIdAndUserId(COMMENT_ID, OTHER_USER_ID)).thenReturn(Optional.of(existingVote));
        when(commentRepository.save(comment)).thenReturn(comment);

        Comment result = commentService.voteComment(COMMENT_ID, OTHER_USER_ID, VoteType.UPVOTE);

        assertEquals(1, result.getScore());
        assertEquals(VoteType.UPVOTE, existingVote.getVoteType());
        verify(commentVoteRepository).save(existingVote);
        verify(commentRepository).save(comment);
    }

    @Test
    void voteComment_blocksSelfVoting() {
        when(commentRepository.findByIdForUpdate(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(comment.getAuthor()));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> commentService.voteComment(COMMENT_ID, AUTHOR_ID, VoteType.UPVOTE)
        );

        assertEquals("Users cannot vote on their own comments", ex.getMessage());
    }

    @Test
    void getCommentsByBugId_returnsScoreOrderedCommentsFromRepository() {
        Long bugId = 55L;
        when(bugRepository.existsById(bugId)).thenReturn(true);
        when(commentRepository.findByBugIdOrderByScoreDescCreatedAtDesc(bugId)).thenReturn(List.of(comment));

        List<Comment> result = commentService.getCommentsByBugId(bugId);

        assertEquals(1, result.size());
        verify(commentRepository).findByBugIdOrderByScoreDescCreatedAtDesc(bugId);
    }

    @Test
    void getCommentsByBugId_throwsWhenBugNotFound() {
        Long missingBugId = 404L;
        when(bugRepository.existsById(missingBugId)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> commentService.getCommentsByBugId(missingBugId)
        );

        assertEquals("Bug not found with id: 404", ex.getMessage());
    }
}

