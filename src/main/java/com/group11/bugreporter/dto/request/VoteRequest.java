package com.group11.bugreporter.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank(message = "voteType is required. Allowed values: UPVOTE, DOWNVOTE.")
    private String voteType;
}
