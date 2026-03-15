package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.request.CommentRequest;
import com.group11.bugreporter.dto.response.CommentResponse;
import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.VoteType;
import com.group11.bugreporter.exception.InvalidVoteTypeException;
import com.group11.bugreporter.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    /**
     * Service responsible for comment business logic and persistence operations.
     */
    private final CommentService commentService;

    /**
     * Creates a new comment for the specified bug.
     *
     * @param bugId    the identifier of the bug to attach the comment to
     * @param authorId the identifier of the user creating the comment
     *                 (currently passed as request parameter; planned to come from SecurityContext)
     * @param payload  request payload containing the comment text and optional image URL
     * @return a {@link ResponseEntity} containing the created {@link CommentResponse} and HTTP 201 status
     */
    @PostMapping("/bug/{bugId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long bugId,
            @RequestParam Long authorId, // todo: use SecurityContext to authorize action
            @Valid @RequestBody CommentRequest payload // this uses the dto request obj to encapsulate the comment data
    ) {
        Comment createdComment = commentService.createComment(bugId, authorId, payload.getText(), payload.getImageUrl());

        return new ResponseEntity<>(CommentResponse.fromEntity(createdComment), HttpStatus.CREATED);
    }

    /**
     * Retrieves all comments associated with a specific bug.
     *
     * @param bugId the identifier of the bug whose comments are requested
     * @return a {@link ResponseEntity} containing the list of comments and HTTP 200 status
     */
    @GetMapping("/bug/{bugId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByBugId(@PathVariable Long bugId) {
        List<Comment> comments = commentService.getCommentsByBugId(bugId);
        List<CommentResponse> response = comments.stream()
                .map(CommentResponse::fromEntity)
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates an existing comment.
     *
     * @param commentId        the identifier of the comment to update
     * @param requestingUserId the identifier of the user requesting the update
     *                         (used for authorization checks in the service layer)
     * @param payload          request payload containing updated comment text and optional image URL
     * @return a {@link ResponseEntity} containing the updated {@link CommentResponse} and HTTP 200 status
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestParam Long requestingUserId, // todo: use SecurityContext to authorize action
            @Valid @RequestBody CommentRequest payload // this uses the dto request obj to encapsulate the comment data (too)
    ) {
        Comment updatedComment = commentService.updateComment(commentId, requestingUserId, payload.getText(), payload.getImageUrl());
        return new ResponseEntity<>(CommentResponse.fromEntity(updatedComment), HttpStatus.OK);
    }

    /**
     * Deletes an existing comment.
     *
     * @param commentId        the identifier of the comment to delete
     * @param requestingUserId the identifier of the user requesting deletion
     *                         (currently passed as request parameter; planned to be derived from SecurityContext)
     * @return a {@link ResponseEntity} with HTTP 204 status when deletion succeeds
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long requestingUserId // todo: requesting user should be derived from authenticated user's SecurityContext
    ) {
        commentService.deleteComment(commentId, requestingUserId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{commentId}/vote")
    public ResponseEntity<CommentResponse> voteComment(
            @PathVariable Long commentId,
            @RequestParam Long requestingUserId, // todo: use SecurityContext to authorize action
            @RequestParam String voteType
    ) {
        if (voteType == null || voteType.isBlank()) {
            throw new InvalidVoteTypeException("voteType is required. Allowed values: UPVOTE, DOWNVOTE.");
        }

        VoteType parsedVoteType;
        try {
            parsedVoteType = VoteType.valueOf(voteType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidVoteTypeException("Invalid voteType '" + voteType + "'. Allowed values: UPVOTE, DOWNVOTE.");
        }

        Comment updatedComment = commentService.voteComment(commentId, requestingUserId, parsedVoteType);
        return new ResponseEntity<>(CommentResponse.fromEntity(updatedComment), HttpStatus.OK);
    }
}
