package com.group11.bugreporter.dto.response;

import com.group11.bugreporter.entity.Bug;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BugResponse {
    private Long id;
    private String title;
    private String text;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;
    private String authorUsername;

    public static BugResponse fromEntity(Bug bug) {
        return BugResponse.builder()
                .id(bug.getId())
                .title(bug.getTitle())
                .text(bug.getText())
                .imageUrl(bug.getImageUrl())
                .status(bug.getStatus().name())
                .createdAt(bug.getCreatedAt())
                .authorUsername(bug.getAuthor().getUsername())
                .build();
    }
}