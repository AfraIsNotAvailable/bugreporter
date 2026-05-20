package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.BugVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BugVoteRepository extends JpaRepository<BugVote, Long> {
    Optional<BugVote> findByBugIdAndUserId(Long bugId, Long userId);
    List<BugVote> findByUserIdAndBugIdIn(Long userId, List<Long> bugIds);
}
