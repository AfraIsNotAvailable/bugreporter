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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

//bean Spring de tip service
//o detecteaza la pornirea aplicatiei
@Service
//asta ii ca lombok sa genereze automat constructor pentru campurile final
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    //service depinde de repository ca sa vorbeasca cu baza de date
    private final UserRepository userRepository;


    //posibil substitute pentru un alt encoder
    //face hash la parole
    private final PasswordEncoder passwordEncoder;


    //asta mi-o trebuit pentru a transforma entitatea interna in dto de response
    //adica din User am mapat sa fie UserResponse
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
    //ii transactional pentru ca scriu in baza de date
    @Transactional
    public UserResponse createUser(UserRequest dto) {
        User user = new User();
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }
        user.setUsername(dto.getUsername());
        //trebuie sa verific inainte daca email-u ii duplicat
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        user.setEmail(dto.getEmail());
        //partea asta poate o modific (ca sa fac un cryping la parole)
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        user.setBanned(false);

        return mapToResponse(userRepository.save(user));
    }

    @Override
    //ii cu baza de date da nu scriu, numa citesc
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        //logica ii ca caut toti userii fac un strem ca sa pot sa mapez sub forma de response si sa le fac lista
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

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new IllegalArgumentException("Username already in use");
            }
            user.setUsername(dto.getUsername());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getPassword() != null) {
            //aici iar poate schimb asta
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserEntityById(id);
        userRepository.delete(user);
    }

    //asta marcheaza user-u ca banned
    @Override
    @Transactional
    public UserResponse banUser(Long id) {
        User user = getUserEntityById(id);
        user.setBanned(true);
        return mapToResponse(userRepository.save(user));
    }


    //face unban la user (posbil sa nu o folosesc pana la urma)
    @Override
    @Transactional
    public UserResponse unbanUser(Long id) {
        User user = getUserEntityById(id);
        user.setBanned(false);
        return mapToResponse(userRepository.save(user));
    }


    //asta intentionez sa o folosesc intern, asa ca ramane User nu mai fac response din el
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



}
