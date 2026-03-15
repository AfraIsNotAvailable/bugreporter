package com.group11.bugreporter.service;

import com.group11.bugreporter.entity.*;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.CommentRepository;
import com.group11.bugreporter.repository.CommentVoteRepository;
import com.group11.bugreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    /**
     * Repository used for persisting and loading {@link Comment} entities.
     */
    private final CommentRepository commentRepository;

    private final CommentVoteRepository commentVoteRepository;

    /**
     * Repository used to validate and load the bug associated with a comment.
     */
    private final BugRepository bugRepository;

    /**
     * Repository used to validate and load the user creating or modifying a comment.
     */
    private final UserRepository userRepository;

    @Transactional
    public Comment voteComment(Long commentId, Long requestingUserId, VoteType voteType) {
        Comment comment = commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User user = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));
        if (comment.getAuthor().getId().equals(requestingUserId)) {
            throw new ForbiddenException("Users cannot vote on their own comments");
        }

        Optional<CommentVote> existingVoteOpt = commentVoteRepository.findByCommentIdAndUserId(commentId, requestingUserId);
        if (existingVoteOpt.isPresent()) {
            CommentVote existingVote = existingVoteOpt.get();
            if (existingVote.getVoteType() == voteType) {
                // User is trying to cast the same vote again, so we remove the existing vote
                commentVoteRepository.delete(existingVote);
                comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? -1 : 1));
            } else {
                // User is changing their vote, so we update the existing vote
                existingVote.setVoteType(voteType);
                commentVoteRepository.save(existingVote);
                comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? 2 : -2));
            }
        } else {
            // No existing vote, so we create a new one
            CommentVote newVote = CommentVote.builder()
                    .comment(comment)
                    .user(user)
                    .voteType(voteType)
                    .build();
            commentVoteRepository.save(newVote);
            comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? 1 : -1));
        }
        return commentRepository.save(comment);
    }

    /**
     * Creates a new comment for a given bug and author.
     *
     * <p>The method first verifies that both the target bug and the author exist.
     * If either is missing, a {@code ResourceNotFoundException} is thrown. Once
     * validated, the method builds a new {@link Comment} entity and persists it.</p>
     *
     * @param bugId    the ID of the bug the comment belongs to
     * @param authorId the ID of the user creating the comment
     * @param text     the textual content of the comment
     * @param image    an optional image reference associated with the comment
     * @return the saved {@link Comment} entity
     * @throws ResourceNotFoundException if the bug or author cannot be found
     */
    @Transactional
    public Comment createComment(Long bugId, Long authorId, String text, String image) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + bugId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + authorId));

        Comment comment = Comment.builder()
                .text(text)
                .imageUrl(image)
                .author(author)
                .bug(bug)
                .build();

        return commentRepository.save(comment);
    }

    /**
     * Retrieves all comments associated with a specific bug, ordered by score descending.
     *
     * <p>Before querying for comments, the method verifies that the bug exists.
     * This avoids returning an empty list for an invalid bug identifier and instead
     * surfaces a domain-specific not-found error.</p>
     *
     * @param bugId the ID of the bug whose comments should be loaded
     * @return a list of comments ordered from highest score to lowest score
     * @throws ResourceNotFoundException if the bug does not exist
     */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByBugId(Long bugId) {
        if (!bugRepository.existsById(bugId)) {
            throw new ResourceNotFoundException("Bug not found with id: " + bugId);
        }

        return commentRepository.findByBugIdOrderByScoreDescCreatedAtDesc(bugId);
    }

    /**
     * Updates a comment's text and optionally its image.
     *
     * <p>Only the original author of the comment is allowed to perform this action.
     * The method first loads the comment, checks ownership, updates the mutable
     * fields, and then saves the modified entity.</p>
     *
     * @param commentId        ID of the comment to update
     * @param requestingUserId ID of the user making the update request; must match the author
     * @param newText          new text for the comment
     * @param newImage         new image URL for the comment; if {@code null}, the existing image is kept
     * @return the updated {@link Comment} object
     * @throws ResourceNotFoundException if the comment cannot be found
     * @throws ForbiddenException     if the requesting user is not the author
     */
    @Transactional
    public Comment updateComment(Long commentId, Long requestingUserId, String newText, String newImage) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        if (!comment.getAuthor().getId().equals(requestingUserId)) {
            throw new ForbiddenException("User is not the author of the comment");
        }

        comment.setText(newText);
        if (newImage != null) {
            comment.setImageUrl(newImage);
        }
        return commentRepository.save(comment);
    }

    /**
     * Deletes a comment if the requesting user is its author.
     *
     * <p>The method validates both existence and ownership before deletion.
     * If the comment does not exist, a not-found exception is thrown. If the
     * requesting user is not the author, an authorization error is raised.</p>
     *
     * @param commentId        ID of the comment to delete
     * @param requestingUserId ID of the user attempting the deletion; must match the author
     * @throws ResourceNotFoundException if the comment cannot be found
     * @throws ForbiddenException     if the requesting user is not the author
     */
    @Transactional
    public void deleteComment(Long commentId, Long requestingUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getAuthor().getId().equals(requestingUserId)) {
            throw new ForbiddenException("User is not the author of the comment");
        }

        commentRepository.delete(comment);
    }
}
