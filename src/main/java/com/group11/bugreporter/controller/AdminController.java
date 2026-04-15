package com.group11.bugreporter.controller;

import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.CommentRepository;
import com.group11.bugreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final BugRepository bugRepository;
    private final CommentRepository commentRepository;

    /**
     * Returneaza lista completa de utilizatori.
     *
     * @return raspuns HTTP 200 cu toti utilizatorii din baza de date
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    /**
     * Returneaza lista completa de comentarii.
     *
     * @return raspuns HTTP 200 cu toate comentariile din baza de date
     */
    @GetMapping("/comments")
    public ResponseEntity<List<Comment>> getAllComments() {
        return ResponseEntity.ok(commentRepository.findAll());
    }

    /**
     * Returneaza lista completa de bug-uri.
     *
     * @return raspuns HTTP 200 cu toate bug-urile din baza de date
     */
    @GetMapping("/bugs")
    public ResponseEntity<List<Bug>> getAllBugs() {
        return ResponseEntity.ok(bugRepository.findAll());
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
    public ResponseEntity<String> setUserBanStatus(@PathVariable Long userId, @RequestParam boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setBanned(banned);
        userRepository.save(user);
        return ResponseEntity.ok("User ban status updated to " + banned);
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
    public ResponseEntity<String> updateUserRole(@PathVariable Long userId, @RequestParam Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setRole(role);
        userRepository.save(user);
        return ResponseEntity.ok("User role updated to " + role.name());
    }

}
