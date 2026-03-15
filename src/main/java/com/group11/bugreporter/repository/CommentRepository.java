package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBugIdOrderByCreatedAtAsc(Long bugId);
}
