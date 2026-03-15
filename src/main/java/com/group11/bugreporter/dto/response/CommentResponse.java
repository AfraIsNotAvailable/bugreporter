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
    private String authorUsername; // optional, for display purposes
    private LocalDateTime createdAt;

    public static CommentResponse fromEntity(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .imageUrl(comment.getImageUrl())
                .authorId(comment.getAuthor().getId())
                // .authorUsername(comment.getAuthor().getUsername()) // todo: add username to User entity and uncomment this line
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
