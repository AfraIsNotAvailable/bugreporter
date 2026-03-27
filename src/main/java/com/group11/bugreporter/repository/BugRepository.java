package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.Bug;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BugRepository extends JpaRepository<Bug, Long> {
    //select *from bugs order by created_at desc
    List<Bug> findAllByOrderByCreatedAtDesc();
}