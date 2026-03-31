package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.request.BugRequest;
import com.group11.bugreporter.dto.response.BugResponse;
import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.BugStatus;
import com.group11.bugreporter.exception.ForbiddenException;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.service.BugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugController {

    private final BugService bugService;
    private final UserRepository userRepository;

    /**
     * Creeaza(raporteaza) un bug nou
     */
    @PostMapping
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
    public ResponseEntity<List<BugResponse>> listAll() {
        List<BugResponse> response = bugService.getAllBugs().stream()
                .map(BugResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Detalii despre un bug
     */
    @GetMapping("/{id}")
    public ResponseEntity<BugResponse> getBugById(@PathVariable Long id) {
        Bug bug = bugService.getBugById(id);
        return ResponseEntity.ok(BugResponse.fromEntity(bug));
    }

    /**
     * actualizeaza titlul sau textul unui bug
     */
    @PutMapping("/{id}")
    public ResponseEntity<BugResponse> updateBug(
            @PathVariable Long id,
            @Valid @RequestBody BugRequest request,
            Authentication authentication) {

        User requestingUser = resolveAuthenticatedUser(authentication);
        Bug updated = bugService.updateBug(id, request, requestingUser.getId());
        return ResponseEntity.ok(BugResponse.fromEntity(updated));
    }

    /**
     * schimba statusul bug -ului
     * valabil doar pentru Moderatori si admini
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
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
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ForbiddenException("Authenticated user not found: " + username));
    }



    @PostMapping("/{id}/tags")
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

        return ResponseEntity.ok(results.stream().map(BugResponse::fromEntity).toList());
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<BugResponse> resolveBug(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User requestingUser = resolveAuthenticatedUser(authentication);
        Bug resolved = bugService.resolveBug(id, requestingUser.getId());
        return ResponseEntity.ok(BugResponse.fromEntity(resolved));
    }
}
