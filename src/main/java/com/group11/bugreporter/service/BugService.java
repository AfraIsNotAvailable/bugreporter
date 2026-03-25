package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.BugRequest;
import com.group11.bugreporter.entity.Bug;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.BugStatus;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BugService {

    private final BugRepository bugRepository;
    private final UserRepository userRepository;

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
     * returneaza toate bugurile raportate
     */
    @Transactional(readOnly = true)
    public List<Bug> getAllBugs() {
        return bugRepository.findAll();
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
    public Bug updateBug(Long id, BugRequest dto) {
        Bug bug = getBugById(id);

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
    public void deleteBug(Long id) {
        if (!bugRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bug not found with id: " + id);
        }
        bugRepository.deleteById(id);
    }
}