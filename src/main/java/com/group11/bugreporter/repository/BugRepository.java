package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.Bug;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BugRepository extends JpaRepository<Bug, Long> {
    //Filtrare dupa tag
    List<Bug> findAllByTags_NameIgnoreCaseOrderByCreatedAtDesc(String tagName);

    //Cautare text in titlu
    List<Bug> findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);

    // Filtrare dupa un anumit utilizator (dupa ID sau username)
    List<Bug> findAllByAuthor_IdOrderByCreatedAtDesc(Long authorId);

    //select *from bugs order by created_at desc
    List<Bug> findAllByOrderByCreatedAtDesc();
}
