package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.BugRequest;
import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.BugVote;
import com.group11.bugreporter.entity.Tag;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.BugStatus;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.entity.enums.VoteType;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.BugVoteRepository;
import com.group11.bugreporter.repository.TagRepository;
import com.group11.bugreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BugService {

    private final BugRepository bugRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final BugVoteRepository bugVoteRepository;

    /**
     * creeaza un bug report nou
     * status initial - mereu open
     */
    @Transactional
    public Bug createBug(BugRequest dto, Long authorId) {
        User author = userRepository.findById(authorId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + authorId));

        Bug bug = Bug.builder().title(dto.getTitle()).text(dto.getText()).imageUrl(dto.getImageUrl()).status(BugStatus.OPEN).author(author).build();

        return bugRepository.save(bug);
    }

    /**
     * cautare bug dupa id
     */
    @Transactional(readOnly = true)
    public Bug getBugById(Long id) {
        return bugRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + id));
    }

    /**
     * Actualizeaza continutul unui bug report
     */
    @Transactional
    public Bug updateBug(Long id, BugRequest request, Long userId, Role role) {
        Bug bug = getBugById(id);

        boolean isAuthor = bug.getAuthor().getId().equals(userId);
        boolean canEdit = isAuthor || role == Role.MODERATOR || role == Role.ADMIN;
        if (!canEdit) {
            throw new ForbiddenException("You are not allowed to edit this bug");
        }

        bug.setTitle(request.getTitle());
        bug.setText(request.getText());

        if (request.getImageUrl() != null) {
            bug.setImageUrl(request.getImageUrl());
        }

        return bugRepository.save(bug);
    }

    /**
     * Schimba statusul unui bug
     */
    @Transactional
    public Bug updateBugStatus(Long bugId, BugStatus newStatus) {
        Bug bug = bugRepository.findById(bugId).orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + bugId));

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
        boolean canDeleteAnyBug = requestingUserRole == Role.MODERATOR || requestingUserRole == Role.ADMIN;

        if (!isAuthor && !canDeleteAnyBug) {
            throw new ForbiddenException("Only the author, a moderator or an administrator can delete this bug report");
        }

        bugRepository.delete(bug);
    }

    @Transactional
    public Bug addTagsToBug(Long bugId, List<String> tagNames) {
        Bug bug = bugRepository.findById(bugId).orElseThrow(() -> new ResourceNotFoundException("Bug not found"));

        Set<Tag> tagEntities = tagNames.stream().map(name -> tagRepository.findByNameIgnoreCase(name.trim()).orElseGet(() -> tagRepository.save(Tag.builder().name(name.trim()).build()))).collect(Collectors.toSet());

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
    public Bug voteBug(Long bugId, Long requestingUserId, VoteType voteType) {
        Bug bug = bugRepository.findByIdForUpdate(bugId)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + bugId));
        User user = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));

        if (bug.getAuthor().getId().equals(requestingUserId)) {
            throw new ForbiddenException("Users cannot vote on their own bugs");
        }

        User author = bug.getAuthor();

        Optional<BugVote> existingVoteOpt = bugVoteRepository.findByBugIdAndUserId(bugId, requestingUserId);
        if (existingVoteOpt.isPresent()) {
            BugVote existingVote = existingVoteOpt.get();
            if (existingVote.getVoteType() == voteType) {
                // removing vote
                bugVoteRepository.delete(existingVote);
                bug.setVoteScore(bug.getVoteScore() + (voteType == VoteType.UPVOTE ? -1 : 1));
                author.setScore(author.getScore() + (voteType == VoteType.UPVOTE ? -2.5 : 1.5));
            } else {
                // flipping vote
                existingVote.setVoteType(voteType);
                bugVoteRepository.save(existingVote);
                bug.setVoteScore(bug.getVoteScore() + (voteType == VoteType.UPVOTE ? 2 : -2));
                author.setScore(author.getScore() + (voteType == VoteType.UPVOTE ? 4.0 : -4.0));
            }
        } else {
            // new vote
            BugVote newVote = BugVote.builder()
                    .bug(bug)
                    .user(user)
                    .voteType(voteType)
                    .build();
            bugVoteRepository.save(newVote);
            bug.setVoteScore(bug.getVoteScore() + (voteType == VoteType.UPVOTE ? 1 : -1));
            author.setScore(author.getScore() + (voteType == VoteType.UPVOTE ? 2.5 : -1.5));
        }

        userRepository.save(author);
        return bugRepository.save(bug);
    }

    @Transactional
    public Bug resolveBug(Long bugId, Long requestingUserId) {
        Bug bug = getBugById(bugId);
        if (!bug.getAuthor().getId().equals(requestingUserId)) {
            throw new ForbiddenException("Doar autorul bug-ului poate confirma rezolvarea acestuia.");
        }

        bug.setStatus(BugStatus.FIXED);
        return bugRepository.save(bug);
    }

}
