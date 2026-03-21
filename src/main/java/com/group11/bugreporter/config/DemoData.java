package com.group11.bugreporter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.Comment;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.CommentRepository;
import com.group11.bugreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoData {

    private static final String DEMO_USERS_PATH =
            "data/PlaceholderDataUsers.json";
    private static final String DEMO_BUGS_PATH =
            "data/PlaceholderDataBugs.json";
    private static final String DEMO_COMMENTS_PATH =
            "data/PlaceholderDataComments.json";
    private static final String DEMO_USERNAME_PREFIX = "demo-user-";
    private static final String DEFAULT_SEED_CREDENTIAL_HASH =
            "$2a$10$rqvTH8sWG7dnD6XJ1kD1D.0hP6Q5XxSxrnPfWZ5fFKj4R.AH4fA7i";

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BugRepository bugRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void loadDemoData() {
        List<Comment> demoComments = getDemoCommentsFromJson();
        Map<Long, Long> authorIdMapping = seedUsers(demoComments);
        Map<Long, Long> bugIdMapping = seedBugs(demoComments);
        seedComments(demoComments, authorIdMapping, bugIdMapping);
    }

    private Map<Long, Long> seedUsers(List<Comment> demoComments) {
        List<User> usersFromJson = getDemoUsersFromJson();

        Map<Long, User> desiredUsersById = new LinkedHashMap<>();
        for (User user : usersFromJson) {
            if (user != null && user.getId() != null) {
                desiredUsersById.put(user.getId(), normalizeUser(user, user.getId()));
            }
        }

        extractAuthorIds(demoComments).forEach(authorId ->
                desiredUsersById.putIfAbsent(authorId, buildFallbackUser(authorId))
        );

        if (desiredUsersById.isEmpty()) {
            log.info("Skipping demo user seeding because no desired users were found.");
            return Map.of();
        }

        Map<Long, Long> userIdMapping = new LinkedHashMap<>();
        int insertedUsers = 0;
        for (Map.Entry<Long, User> entry : desiredUsersById.entrySet()) {
            Long sourceId = entry.getKey();
            User template = entry.getValue();

            if (userRepository.existsById(sourceId)) {
                userIdMapping.put(sourceId, sourceId);
                continue;
            }

            Long existingByUsername = userRepository.findByUsername(template.getUsername())
                    .map(User::getId)
                    .orElse(null);
            if (existingByUsername != null) {
                userIdMapping.put(sourceId, existingByUsername);
                continue;
            }

            User saved = userRepository.save(template);
            insertedUsers++;
            userIdMapping.put(sourceId, saved.getId());
        }

        if (insertedUsers == 0) {
            log.info("Skipping demo user inserts because users already exist.");
        } else {
            log.info("Seeded {} demo users.", insertedUsers);
        }
        return userIdMapping;
    }

    private Map<Long, Long> seedBugs(List<Comment> demoComments) {
        List<Bug> bugsFromJson = getDemoBugsFromJson();

        Map<Long, Bug> desiredBugsById = new LinkedHashMap<>();
        for (Bug bug : bugsFromJson) {
            if (bug != null && bug.getId() != null) {
                desiredBugsById.put(bug.getId(), bug);
            }
        }

        extractBugIds(demoComments).forEach(bugId ->
                desiredBugsById.putIfAbsent(bugId, buildFallbackBug(bugId))
        );

        if (desiredBugsById.isEmpty()) {
            log.info("Skipping demo bug seeding because no desired bugs were found.");
            return Map.of();
        }

        Map<Long, Long> bugIdMapping = new LinkedHashMap<>();
        int insertedBugs = 0;
        for (Long sourceId : desiredBugsById.keySet()) {
            if (bugRepository.existsById(sourceId)) {
                bugIdMapping.put(sourceId, sourceId);
                continue;
            }

            Bug saved = bugRepository.save(new Bug());
            insertedBugs++;
            bugIdMapping.put(sourceId, saved.getId());
        }

        if (insertedBugs == 0) {
            log.info("Skipping demo bug inserts because bugs already exist.");
        } else {
            log.info("Seeded {} demo bugs.", insertedBugs);
        }
        return bugIdMapping;
    }

    private void seedComments(List<Comment> demoComments, Map<Long, Long> authorIdMapping, Map<Long, Long> bugIdMapping) {
        if (commentRepository.count() > 0) {
            log.info("Skipping demo comment seeding because comments already exist.");
            return;
        }

        if (demoComments.isEmpty()) {
            log.info("Skipping demo comment seeding because demo dataset is empty.");
            return;
        }

        Set<Long> unresolvedAuthors = extractAuthorIds(demoComments).stream()
                .filter(authorId -> !authorIdMapping.containsKey(authorId))
                .collect(Collectors.toSet());

        Set<Long> unresolvedBugs = extractBugIds(demoComments).stream()
                .filter(bugId -> !bugIdMapping.containsKey(bugId))
                .collect(Collectors.toSet());

        if (!unresolvedAuthors.isEmpty() || !unresolvedBugs.isEmpty()) {
            log.warn("Some demo comments still reference unresolved users/bugs; invalid entries will be skipped.");
        }

        List<Comment> validComments = new ArrayList<>();
        for (Comment comment : demoComments) {
            Long authorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;
            Long bugId = comment.getBug() != null ? comment.getBug().getId() : null;
            Long mappedAuthorId = authorId != null ? authorIdMapping.get(authorId) : null;
            Long mappedBugId = bugId != null ? bugIdMapping.get(bugId) : null;

            if (mappedAuthorId == null || mappedBugId == null) {
                continue;
            }

            // Keep referential integrity and let the DB generate fresh IDs.
            comment.setId(null);
            comment.setAuthor(userRepository.getReferenceById(mappedAuthorId));
            comment.setBug(bugRepository.getReferenceById(mappedBugId));
            validComments.add(comment);
        }

        if (validComments.isEmpty()) {
            log.warn("Skipping demo comment seeding because no valid comments remain after validation.");
            return;
        }

        commentRepository.saveAll(validComments);
        log.info("Seeded {} demo comments.", validComments.size());
    }

    private Set<Long> extractAuthorIds(List<Comment> comments) {
        return comments.stream()
                .map(Comment::getAuthor)
                .filter(author -> author != null && author.getId() != null)
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    private Set<Long> extractBugIds(List<Comment> comments) {
        return comments.stream()
                .map(Comment::getBug)
                .filter(bug -> bug != null && bug.getId() != null)
                .map(Bug::getId)
                .collect(Collectors.toSet());
    }

    private User normalizeUser(User user, Long fallbackId) {
        User normalized = new User();
        normalized.setId(null);
        normalized.setUsername(hasText(user.getUsername()) ? user.getUsername() : DEMO_USERNAME_PREFIX + fallbackId);
        normalized.setEmail(hasText(user.getEmail()) ? user.getEmail() : DEMO_USERNAME_PREFIX + fallbackId + "@example.com");
        normalized.setPassword(hasText(user.getPassword()) ? user.getPassword() : DEFAULT_SEED_CREDENTIAL_HASH);
        normalized.setRole(user.getRole() != null ? user.getRole() : Role.USER);
        normalized.setBanned(user.isBanned());
        normalized.setCreatedAt(user.getCreatedAt());
        return normalized;
    }

    private User buildFallbackUser(Long userId) {
        User user = new User();
        user.setId(null);
        user.setUsername(DEMO_USERNAME_PREFIX + userId);
        user.setEmail(DEMO_USERNAME_PREFIX + userId + "@example.com");
        user.setPassword(DEFAULT_SEED_CREDENTIAL_HASH);
        user.setRole(Role.USER);
        user.setBanned(false);
        return user;
    }

    private Bug buildFallbackBug(Long bugId) {
        Bug bug = new Bug();
        bug.setId(null);
        return bug;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<User> getDemoUsersFromJson() {
        return readListFromJson(DEMO_USERS_PATH, User.class);
    }

    private List<Bug> getDemoBugsFromJson() {
        return readListFromJson(DEMO_BUGS_PATH, Bug.class);
    }

    private List<Comment> getDemoCommentsFromJson() {
        return readListFromJson(DEMO_COMMENTS_PATH, Comment.class);
    }

    private <T> List<T> readListFromJson(String path, Class<T> itemType) {
        ClassPathResource resource = new ClassPathResource(path);
        try {
            try (var inputStream = resource.getInputStream()) {
                return objectMapper.readValue(
                        inputStream,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, itemType)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to read demo data from '{}': {}", path, e.getMessage());
            return List.of();
        }
    }
}
