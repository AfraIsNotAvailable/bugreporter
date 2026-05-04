package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.CommentVote;
import com.group11.bugreporter.entity.enums.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {
    Optional<CommentVote> findByCommentIdAndUserId(Long commentId, Long userId);
    List<CommentVote> findByUserIdAndCommentIdIn(Long userId, List<Long> commentIds);
}
