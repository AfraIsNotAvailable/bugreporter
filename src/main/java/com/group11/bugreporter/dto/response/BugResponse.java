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
        if (bug == null) return null;

        return BugResponse.builder()
                .id(bug.getId())
                .title(bug.getTitle())
                .text(bug.getText())
                .imageUrl(bug.getImageUrl())
                // DEFAULT: Daca statusul e null, scrie "UNKNOWN"
                .status(bug.getStatus() != null ? bug.getStatus().name() : "PENDING")
                // DEFAULT: Daca n-are data, pune ora curenta (sau lasa null daca vrei)
                .createdAt(bug.getCreatedAt() != null ? bug.getCreatedAt() : LocalDateTime.now())
                // DEFAULT: Daca n-are autor, scrie "Anonymous"
                .authorUsername(bug.getAuthor() != null ? bug.getAuthor().getUsername() : "Anonymous")
                .tags(bug.getTags() != null ? bug.getTags().stream()
                                              .map(tag -> tag.getName())
                                              .collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet())
                .build();
    }
}