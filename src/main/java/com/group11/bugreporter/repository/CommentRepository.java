package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.Comment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBugIdOrderByScoreDescCreatedAtDesc(Long bugId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id = :commentId")
    Optional<Comment> findByIdForUpdate(@Param("commentId") Long commentId);
}
