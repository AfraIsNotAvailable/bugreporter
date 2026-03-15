package com.group11.bugreporter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class CommentRequest {

    @NotBlank(message = "Comment text cannot be blank")
    @Size(max = 2000, message = "Comment text cannot exceed 2000 characters")
    private String text;

    @URL(message = "Image URL must be a valid URL")
    private String imageUrl;
}
