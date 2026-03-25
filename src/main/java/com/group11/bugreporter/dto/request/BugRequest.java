package com.group11.bugreporter.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BugRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title is too long")
    private String title;

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 5000, message = "Description is too long")
    private String text;

    private String imageUrl;
}