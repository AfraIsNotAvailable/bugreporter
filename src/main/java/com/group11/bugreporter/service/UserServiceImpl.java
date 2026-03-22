package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.UserRequest;
import com.group11.bugreporter.dto.response.UserResponse;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;



    /*
    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setBanned(user.isBanned());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }


    @Override
    @Transactional
    public UserResponse createUser(UserRequest dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setRole(Role.USER);
        user.setBanned(false);

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return mapToResponse(getUserEntityById(id));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest dto) {
        User user = getUserEntityById(id);

        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            user.setPassword(dto.getPassword());
        }

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserEntityById(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse banUser(Long id) {
        User user = getUserEntityById(id);
        user.setBanned(true);
        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse unbanUser(Long id) {
        User user = getUserEntityById(id);
        user.setBanned(false);
        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }


     */
}
