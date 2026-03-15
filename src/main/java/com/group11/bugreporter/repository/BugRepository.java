package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.Bug;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BugRepository extends JpaRepository<Bug, Long> {
}
