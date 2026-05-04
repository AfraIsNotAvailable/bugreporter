package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.response.BugResponse;
import com.group11.bugreporter.dto.response.CommentResponse;
import com.group11.bugreporter.dto.response.UserResponse;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.CommentRepository;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final BugRepository bugRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    /**
     * Returneaza lista completa de utilizatori.
     *
     * @return raspuns HTTP 200 cu toti utilizatorii din baza de date
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Returneaza lista completa de comentarii.
     *
     * @return raspuns HTTP 200 cu toate comentariile din baza de date
     */
    @GetMapping("/comments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommentResponse>> getAllComments() {
        return ResponseEntity.ok(commentRepository.findAll().stream()
                .map(CommentResponse::fromEntity)
                .toList());
    }

    /**
     * Returneaza lista completa de bug-uri.
     *
     * @return raspuns HTTP 200 cu toate bug-urile din baza de date
     */
    @GetMapping("/bugs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BugResponse>> getAllBugs() {
        return ResponseEntity.ok(bugRepository.findAll().stream()
                .map(BugResponse::fromEntity)
                .toList());
    }

    /**
     * Actualizeaza statusul de ban pentru un utilizator.
     *
     * @param userId id-ul utilizatorului care trebuie actualizat
     * @param banned noua valoare pentru statusul de ban (true/false)
     * @return raspuns HTTP 200 cu mesaj de confirmare
     * @throws RuntimeException daca utilizatorul nu este gasit
     */
    @PutMapping("/users/{userId}/ban")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<UserResponse> setUserBanStatus(@PathVariable Long userId, @RequestParam boolean banned) {
        UserResponse response = banned ? userService.banUser(userId) : userService.unbanUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualizeaza rolul unui utilizator.
     *
     * @param userId id-ul utilizatorului pentru care se modifica rolul
     * @param role noul rol care va fi setat (USER, MODERATOR, ADMIN)
     * @return raspuns HTTP 200 cu mesaj de confirmare
     * @throws RuntimeException daca utilizatorul nu este gasit
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long userId, @RequestParam Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(role);
        return ResponseEntity.ok(mapToResponse(userRepository.save(user)));
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setBanned(user.isBanned());
        response.setCreatedAt(user.getCreatedAt());
        response.setPhoneNumber(user.getPhoneNumber());
        return response;
    }
}
