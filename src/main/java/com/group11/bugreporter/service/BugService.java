package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.BugRequest;
import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.Tag;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.BugStatus;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.TagRepository;
import com.group11.bugreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BugService {

    private final BugRepository bugRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    /**
     * creeaza un bug report nou
     * status initial - mereu open
     */
    @Transactional
    public Bug createBug(BugRequest dto, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + authorId));

        Bug bug = Bug.builder()
                .title(dto.getTitle())
                .text(dto.getText())
                .imageUrl(dto.getImageUrl())
                .status(BugStatus.OPEN)
                .author(author)
                .build();

        return bugRepository.save(bug);
    }

    /**
     * cautare bug dupa id
     */
    @Transactional(readOnly = true)
    public Bug getBugById(Long id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + id));
    }

    /**
     * Actualizeaza continutul unui bug report
     */
    @Transactional
    public Bug updateBug(Long id, BugRequest dto, Long requestingUserId, Role requestingUserRole) {
        Bug bug = getBugById(id);

        boolean isAuthor = bug.getAuthor().getId().equals(requestingUserId);
        boolean isPrivileged = requestingUserRole == Role.ADMIN || requestingUserRole == Role.MODERATOR;
        if (!isAuthor && !isPrivileged) {
            throw new ForbiddenException("You are not the author of this bug report");
        }

        bug.setTitle(dto.getTitle());
        bug.setText(dto.getText());
        if (dto.getImageUrl() != null) {
            bug.setImageUrl(dto.getImageUrl());
        }

        return bugRepository.save(bug);
    }

    /**
     * Schimba statusul unui bug
     */
    @Transactional
    public Bug updateBugStatus(Long bugId, BugStatus newStatus) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + bugId));

        bug.setStatus(newStatus);
        return bugRepository.save(bug);
    }

    /**
     * Sterge un bug
     */
    @Transactional
    public void deleteBug(Long id, Long requestingUserId, Role requestingUserRole) {
        Bug bug = getBugById(id);

        boolean isAuthor = bug.getAuthor().getId().equals(requestingUserId);
        boolean isPrivileged = requestingUserRole == Role.ADMIN || requestingUserRole == Role.MODERATOR;

        if (!isAuthor && !isPrivileged) {
            throw new ForbiddenException("Only the author, a moderator, or an admin can delete this bug report");
        }

        bugRepository.delete(bug);
    }

    @Transactional
    public Bug addTagsToBug(Long bugId, List<String> tagNames) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found"));

        Set<Tag> tagEntities = tagNames.stream()
                .map(name -> tagRepository.findByNameIgnoreCase(name.trim())
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name.trim()).build())))
                .collect(Collectors.toSet());

        bug.getTags().addAll(tagEntities);
        return bugRepository.save(bug);
    }

    /**
     * returneaza toate bugurile raportate
     */
    @Transactional(readOnly = true)
    public List<Bug> getAllBugs() {
        return bugRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Bug> getBugsByTag(String tagName) {
        return bugRepository.findAllByTags_NameIgnoreCaseOrderByCreatedAtDesc(tagName);
    }

    @Transactional(readOnly = true)
    public List<Bug> searchByTitle(String title) {
        return bugRepository.findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc(title);
    }

    @Transactional(readOnly = true)
    public List<Bug> getBugsByAuthor(Long authorId) {
        return bugRepository.findAllByAuthor_IdOrderByCreatedAtDesc(authorId);
    }

    @Transactional
    public Bug resolveBug(Long bugId, Long requestingUserId, Role requestingUserRole) {
        Bug bug = getBugById(bugId);

        boolean isAuthor = bug.getAuthor().getId().equals(requestingUserId);
        boolean isPrivileged = requestingUserRole == Role.ADMIN || requestingUserRole == Role.MODERATOR;
        if (!isAuthor && !isPrivileged) {
            throw new ForbiddenException("Only the author, a moderator, or an admin can resolve this bug report");
        }

        bug.setStatus(BugStatus.FIXED);
        return bugRepository.save(bug);
    }

}
