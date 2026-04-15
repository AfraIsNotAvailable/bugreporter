### 1. What is `@UniqueConstraint`?

`@UniqueConstraint` is a JPA annotation that tells Hibernate to generate a unique index constraint at the database level for a specific combination of columns.

By writing `@UniqueConstraint(columnNames = {"comment_id", "user_id"})`, you are telling the PostgreSQL database: *"Never allow two rows in this table to have the exact same `comment_id` and `user_id`."* This mathematically guarantees that a user can never double-vote on the same comment, even if a bug in the code or a concurrent API request tries to insert a second vote.

### 2. Should `CommentVote` use an Enum?

**Yes, absolutely.** Given the complex scoring rules from your `README.md` (*+5 pt for upvoted comment, -2.5 pt for downvoted comment, etc.*), using an Enum makes the code much more readable and scalable than a boolean.

We don't necessarily need a `NONE` value in the database, because if a user "removes" their vote, the standard practice is to just delete the `CommentVote` row entirely. However, we will use a `VoteType` enum for `UPVOTE` and `DOWNVOTE`.

### 3. How to handle the `canVote` check without breaking `CommentResponse::fromEntity`?

The absolute best and most standard RESTful way to handle this is to **not calculate `canVote` on the backend at all**.

Since your `CommentResponse` already sends the `authorId`, and your React frontend already knows the ID of the currently logged-in user, the frontend can just do this natively:

```jsx
// Inside your React component
const canVote = currentUser.id !== comment.authorId;

{canVote && (
    <div>
        <button onClick={() => handleVote('UPVOTE')}>👍</button>
        <button onClick={() => handleVote('DOWNVOTE')}>👎</button>
    </div>
)}

```

This keeps your DTOs perfectly stateless, avoids needing `requestingUserId` for basic `GET` requests, and allows you to keep using the clean `comments.stream().map(CommentResponse::fromEntity).toList();` in your controller.

---

### The Rewritten Code

Here is the updated implementation based on these specifications:

#### 1. Create the Enum `VoteType`

Create `src/main/java/com/group11/bugreporter/entity/VoteType.java`:

```java
package com.group11.bugreporter.entity;

public enum VoteType {
    UPVOTE,
    DOWNVOTE
}

```

#### 2. Create the `CommentVote` Entity

Create `src/main/java/com/group11/bugreporter/entity/CommentVote.java`:

```java
package com.group11.bugreporter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"comment_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;
}

```

#### 3. Revert `CommentResponse` to its clean state

Add the `score` field, but leave out `canVote` so we don't break the method signature.

```java
package com.group11.bugreporter.dto.response;

import com.group11.bugreporter.entity.Comment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class CommentResponse {
    private Long id;
    private String text;
    private String imageUrl;
    private Long authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private Integer score; // Added score for the comment

    public static CommentResponse fromEntity(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .imageUrl(comment.getImageUrl())
                .authorId(comment.getAuthor().getId())
                // .authorUsername(comment.getAuthor().getUsername())
                .createdAt(comment.getCreatedAt())
                .score(comment.getScore() != null ? comment.getScore() : 0) // Safe null check
                .build();
    }
}

```

#### 4. Update `CommentService` with the Enum logic

*(Note: I added `private Integer score = 0;` to the `Comment` entity under the hood as previously discussed).*

```java
import com.group11.bugreporter.entity.VoteType;
import com.group11.bugreporter.entity.CommentVote;
import com.group11.bugreporter.repository.CommentVoteRepository;
import java.util.Optional;

// ... inside CommentService ...

    private final CommentVoteRepository commentVoteRepository;

    @Transactional
    public Comment voteComment(Long commentId, Long requestingUserId, VoteType voteType) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User user = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));

        if (comment.getAuthor().getId().equals(requestingUserId)) {
            throw new ForbiddenException("You cannot vote on your own comment.");
        }

        Optional<CommentVote> existingVoteOpt = commentVoteRepository.findByCommentIdAndUserId(commentId, requestingUserId);

        if (existingVoteOpt.isPresent()) {
            CommentVote existingVote = existingVoteOpt.get();
            
            if (existingVote.getType() == voteType) {
                // Toggle off: User clicked the exact same vote again, so we delete it
                commentVoteRepository.delete(existingVote);
                comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? -1 : 1));
            } else {
                // Change vote: User changed from UPVOTE to DOWNVOTE (or vice-versa)
                existingVote.setType(voteType);
                comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? 2 : -2));
            }
        } else {
            // Brand new vote
            CommentVote newVote = CommentVote.builder()
                    .comment(comment)
                    .user(user)
                    .type(voteType)
                    .build();
            commentVoteRepository.save(newVote);
            comment.setScore(comment.getScore() + (voteType == VoteType.UPVOTE ? 1 : -1));
        }

        return commentRepository.save(comment);
    }

```

#### 5. Update `CommentController`

Notice how the GET mapping stays perfectly clean, just like you wanted!

```java
import com.group11.bugreporter.entity.VoteType;

    // ... existing GET endpoint ...
    @GetMapping("/bug/{bugId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByBugId(@PathVariable Long bugId) {
        List<Comment> comments = commentService.getCommentsByBugId(bugId);
        List<CommentResponse> response = comments.stream()
                .map(CommentResponse::fromEntity) // Stays clean!
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Upvotes or downvotes a comment.
     *
     * @param commentId        the identifier of the comment to vote on
     * @param requestingUserId the user casting the vote
     * @param voteType         "UPVOTE" or "DOWNVOTE"
     */
    @PostMapping("/{commentId}/vote")
    public ResponseEntity<CommentResponse> voteComment(
            @PathVariable Long commentId,
            @RequestParam Long requestingUserId, // todo: use SecurityContext later
            @RequestParam VoteType voteType
    ) {
        Comment updatedComment = commentService.voteComment(commentId, requestingUserId, voteType);
        return new ResponseEntity<>(CommentResponse.fromEntity(updatedComment), HttpStatus.OK);
    }

```