package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.request.BugRequest;
import com.group11.bugreporter.dto.request.VoteRequest;
import com.group11.bugreporter.dto.response.BugResponse;
import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.BugVote;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.BugStatus;
import com.group11.bugreporter.entity.enums.VoteType;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.exception.InvalidVoteTypeException;
import com.group11.bugreporter.repository.BugVoteRepository;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.service.BugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugController {

    private final BugService bugService;
    private final UserRepository userRepository;
    private final BugVoteRepository bugVoteRepository;

    /**
     * Creeaza(raporteaza) un bug nou
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugResponse> reportBug(
            @Valid @RequestBody BugRequest request,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        Bug bug = bugService.createBug(request, user.getId());
        return new ResponseEntity<>(BugResponse.fromEntity(bug), HttpStatus.CREATED);
    }

    /**
     * Lista cu toate bugurile
     */
    @GetMapping
    public ResponseEntity<List<BugResponse>> listAll(Authentication authentication) {
        List<Bug> bugs = bugService.getAllBugs();
        return ResponseEntity.ok(mapBugsWithVotes(bugs, authentication));
    }

    /**
     * Detalii despre un bug
     */
    @GetMapping("/{id}")
    public ResponseEntity<BugResponse> getBugById(@PathVariable Long id, Authentication authentication) {
        Bug bug = bugService.getBugById(id);
        Long userId = resolveOptionalUserId(authentication);
        VoteType userVote = userId != null
                ? bugVoteRepository.findByBugIdAndUserId(id, userId).map(BugVote::getVoteType).orElse(null)
                : null;
        return ResponseEntity.ok(BugResponse.fromEntity(bug, userVote));
    }

    /**
     * actualizeaza titlul sau textul unui bug
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugResponse> updateBug(
            @PathVariable Long id,
            @Valid @RequestBody BugRequest request,
            Authentication authentication) {

        User requestingUser = resolveAuthenticatedUser(authentication);
        Bug updated = bugService.updateBug(id, request, requestingUser.getId(), requestingUser.getRole());
        return ResponseEntity.ok(BugResponse.fromEntity(updated));
    }

    /**
     * schimba statusul bug -ului
     * valabil doar pentru Moderatori
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BugResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam BugStatus status
    ) {
        Bug updated = bugService.updateBugStatus(id, status);
        return ResponseEntity.ok(BugResponse.fromEntity(updated));
    }

    /**
     * sterge un bug
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteBug(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        bugService.deleteBug(id, requestingUser.getId(), requestingUser.getRole());

        return ResponseEntity.noContent().build();
    }

    /**
     * Helper pentru a identifica user-ul din token-ul JWT.
     */
    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Authentication is required");
        }
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof User userPrincipal) {
            username = userPrincipal.getUsername();
        } else if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ForbiddenException("Authenticated user not found: " + username));
    }



    @PostMapping("/{id}/tags")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugResponse> addTags(
            @PathVariable Long id,
            @RequestBody List<String> tags
    ) {
        Bug updatedBug = bugService.addTagsToBug(id, tags);
        return ResponseEntity.ok(BugResponse.fromEntity(updatedBug));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<BugResponse>> filterBugs(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) boolean mine,
            Authentication auth
    ) {
        List<Bug> results;

        if (mine) {
            User user = resolveAuthenticatedUser(auth);
            results = bugService.getBugsByAuthor(user.getId());
        } else if (tag != null) {
            results = bugService.getBugsByTag(tag);
        } else if (search != null) {
            results = bugService.searchByTitle(search);
        } else if (userId != null) {
            results = bugService.getBugsByAuthor(userId);
        } else {
            results = bugService.getAllBugs();
        }

        return ResponseEntity.ok(mapBugsWithVotes(results, auth));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugResponse> resolveBug(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        Bug resolved = bugService.resolveBug(id, requestingUser.getId());
        return ResponseEntity.ok(BugResponse.fromEntity(resolved));
    }

    @PostMapping("/{id}/vote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugResponse> voteBug(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody VoteRequest payload
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        String voteType = payload.getVoteType();

        if (voteType == null || voteType.isBlank()) {
            throw new InvalidVoteTypeException("voteType is required. Allowed values: UPVOTE, DOWNVOTE.");
        }

        VoteType parsedVoteType;
        try {
            parsedVoteType = VoteType.valueOf(voteType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidVoteTypeException("Invalid voteType '" + voteType + "'. Allowed values: UPVOTE, DOWNVOTE.");
        }

        Bug updated = bugService.voteBug(id, requestingUser.getId(), parsedVoteType);
        BugVote userVote = bugVoteRepository.findByBugIdAndUserId(id, requestingUser.getId()).orElse(null);
        return ResponseEntity.ok(BugResponse.fromEntity(updated, userVote != null ? userVote.getVoteType() : null));
    }

    private Long resolveOptionalUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByUsername(authentication.getName()).map(User::getId).orElse(null);
    }

    private List<BugResponse> mapBugsWithVotes(List<Bug> bugs, Authentication authentication) {
        Long userId = resolveOptionalUserId(authentication);
        if (userId == null || bugs.isEmpty()) {
            return bugs.stream().map(BugResponse::fromEntity).toList();
        }
        List<Long> bugIds = bugs.stream().map(Bug::getId).toList();
        Map<Long, VoteType> voteMap = bugVoteRepository.findByUserIdAndBugIdIn(userId, bugIds)
                .stream()
                .collect(Collectors.toMap(v -> v.getBug().getId(), BugVote::getVoteType));
        return bugs.stream()
                .map(b -> BugResponse.fromEntity(b, voteMap.get(b.getId())))
                .toList();
    }
}
