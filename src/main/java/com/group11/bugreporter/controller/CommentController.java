package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.request.CommentRequest;
import com.group11.bugreporter.dto.request.VoteRequest;
import com.group11.bugreporter.dto.response.CommentResponse;
import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.VoteType;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.exception.InvalidVoteTypeException;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final UserRepository userRepository;

    /**
     * Creates a new comment for the specified bug.
     *
     * @param bugId    the identifier of the bug to attach the comment to
     * @param authentication authenticated principal resolved from SecurityContext
     * @param payload  request payload containing the comment text and optional image URL
     * @return a {@link ResponseEntity} containing the created {@link CommentResponse} and HTTP 201 status
     */
    @PostMapping("/bug/{bugId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long bugId,
            Authentication authentication,
            @Valid @RequestBody CommentRequest payload // this uses the dto request obj to encapsulate the comment data
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        Comment createdComment = commentService.createComment(
                bugId,
                requestingUser.getId(),
                payload.getText(),
                payload.getImageUrl());

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
     * @param authentication   authenticated principal resolved from SecurityContext
     * @param payload          request payload containing updated comment text and optional image URL
     * @return a {@link ResponseEntity} containing the updated {@link CommentResponse} and HTTP 200 status
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @Valid @RequestBody CommentRequest payload // this uses the dto request obj to encapsulate the comment data (too)
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        Comment updatedComment = commentService.updateComment(
                commentId,
                requestingUser.getId(),
                requestingUser.getRole(),
                payload.getText(),
                payload.getImageUrl());
        return new ResponseEntity<>(CommentResponse.fromEntity(updatedComment), HttpStatus.OK);
    }

    /**
     * Deletes an existing comment.
     *
     * @param commentId        the identifier of the comment to delete
     * @param authentication   authenticated principal resolved from SecurityContext
     * @return a {@link ResponseEntity} with HTTP 204 status when deletion succeeds
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        commentService.deleteComment(commentId, requestingUser.getId(), requestingUser.getRole());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{commentId}/vote")
    public ResponseEntity<CommentResponse> voteComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @Valid @RequestBody VoteRequest payload
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        String voteType = payload.getVoteType();

        if (voteType == null || voteType.isBlank()) {
            throw new InvalidVoteTypeException("voteType is required. Allowed values: UPVOTE, DOWNVOTE.");
        }

        VoteType parsedVoteType;
        try {
            parsedVoteType = VoteType.valueOf(voteType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidVoteTypeException("Invalid voteType '" + voteType + "'. Allowed values: UPVOTE, DOWNVOTE.");
        }

        Comment updatedComment = commentService.voteComment(commentId, requestingUser.getId(), parsedVoteType);
        return new ResponseEntity<>(CommentResponse.fromEntity(updatedComment), HttpStatus.OK);
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Authentication is required");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ForbiddenException("Authenticated user not found: " + username));
    }
}
