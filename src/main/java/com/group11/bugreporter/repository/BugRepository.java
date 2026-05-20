package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.Bug;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BugRepository extends JpaRepository<Bug, Long> {
    //Filtrare dupa tag
    List<Bug> findAllByTags_NameIgnoreCaseOrderByCreatedAtDesc(String tagName);

    //Cautare text in titlu
    List<Bug> findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);

    // Filtrare dupa un anumit utilizator (dupa ID sau username)
    List<Bug> findAllByAuthor_IdOrderByCreatedAtDesc(Long authorId);

    //select *from bugs order by created_at desc
    List<Bug> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bug b where b.id = :bugId")
    Optional<Bug> findByIdForUpdate(@Param("bugId") Long bugId);
}
