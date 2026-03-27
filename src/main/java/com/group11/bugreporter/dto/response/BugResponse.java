package com.group11.bugreporter.dto.response;

import com.group11.bugreporter.entity.Bug;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

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
    private Set<String> tags;

    public static BugResponse fromEntity(Bug bug) {
        return BugResponse.builder()
                .id(bug.getId())
                .title(bug.getTitle())
                .text(bug.getText())
                .imageUrl(bug.getImageUrl())
                .status(bug.getStatus().name())
                .createdAt(bug.getCreatedAt())
                .authorUsername(bug.getAuthor().getUsername())
                .tags(bug.getTags() != null ? bug.getTags().stream()
                        .map(tag -> tag.getName())
                        .collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet())
                .build();
    }
}