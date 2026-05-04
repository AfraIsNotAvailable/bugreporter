package com.group11.bugreporter.dto.response;

import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.enums.VoteType;
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
    private String authorUsername; // optional, for display purposes
    private LocalDateTime createdAt;
    private Integer score;
    private String userVote; // null = note voted or not authenticated

    public static CommentResponse fromEntity(Comment comment) {
        return fromEntity(comment, null);
    }

    public static CommentResponse fromEntity(Comment comment, VoteType userVote) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .imageUrl(comment.getImageUrl())
                .authorId(comment.getAuthor().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .createdAt(comment.getCreatedAt())
                .score(comment.getScore())
                .userVote(userVote != null ? userVote.name() : null)
                .build();
    }
}
