package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.UserRequest;
import com.group11.bugreporter.dto.response.UserResponse;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//am pus adnotarea asta ca sa mearga mock si injectmocks
//folosesc mock pentru ca nu vreau sa depind de baza de date
//ca sa controlez exact ce raspund dependintele
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    //asta inseamna ca creeaza obiecte false pentru dependinte
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    //face instanta a clasei testate (care ii implementarea de service)
    //si baga mock-urile
    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserRequest userRequest;

    //asta inseamna ca se ruleaza inainte de fiecare test
    //asta ii buna ca testele sa nu se influenteze intre ele
    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("alex")
                .email("alex@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .banned(false)
                .createdAt(LocalDateTime.now())
                .build();

        userRequest = new UserRequest();
        userRequest.setUsername("alex");
        userRequest.setEmail("alex@test.com");
        userRequest.setPassword("123456");
    }

    //marcheaza metoda ca fiind de test
    @Test
    void shouldCreateUser() {
        //when-urile definesc ce raspunde un mock
        when(userRepository.existsByUsername("alex")).thenReturn(false);
        when(userRepository.existsByEmail("alex@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        //apelez metoda testata
        UserResponse response = userService.createUser(userRequest);

        //toate assert-urile sunde de fapt verificari
        assertNotNull(response);
        assertEquals("alex", response.getUsername());
        assertEquals("alex@test.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        assertFalse(response.isBanned());


        //verify-urile verifica daca o metoda o fost apelata
        verify(userRepository).existsByUsername("alex");
        verify(userRepository).existsByEmail("alex@test.com");
        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExistsOnCreate() {
        when(userRepository.existsByUsername("alex")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(userRequest)
        );

        assertEquals("Username already in use", ex.getMessage());
        verify(userRepository).existsByUsername("alex");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyExistsOnCreate() {
        when(userRepository.existsByUsername("alex")).thenReturn(false);
        when(userRepository.existsByEmail("alex@test.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(userRequest)
        );

        assertEquals("Email already in use", ex.getMessage());
        verify(userRepository).existsByUsername("alex");
        verify(userRepository).existsByEmail("alex@test.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("alex", result.get(0).getUsername());
        assertEquals("alex@test.com", result.get(0).getEmail());

        verify(userRepository).findAll();
    }

    @Test
    void shouldReturnUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("alex", response.getUsername());

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenUserNotFoundById() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldUpdateUser() {
        UserRequest updateRequest = new UserRequest();
        updateRequest.setUsername("alexandra");
        updateRequest.setEmail("alexandra@test.com");
        updateRequest.setPassword("newpass");

        User updatedUser = User.builder()
                .id(1L)
                .username("alexandra")
                .email("alexandra@test.com")
                .password("newEncodedPass")
                .role(Role.USER)
                .banned(false)
                .createdAt(user.getCreatedAt())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("alexandra")).thenReturn(false);
        when(userRepository.existsByEmail("alexandra@test.com")).thenReturn(false);
        when(passwordEncoder.encode("newpass")).thenReturn("newEncodedPass");
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserResponse response = userService.updateUser(1L, updateRequest);

        assertNotNull(response);
        assertEquals("alexandra", response.getUsername());
        assertEquals("alexandra@test.com", response.getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("alexandra");
        verify(userRepository).existsByEmail("alexandra@test.com");
        verify(passwordEncoder).encode("newpass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void shouldBanUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.banUser(1L);

        assertTrue(response.isBanned());
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void shouldUnbanUser() {
        user.setBanned(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.unbanUser(1L);

        assertFalse(response.isBanned());
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void shouldFindUserByEmail() {
        when(userRepository.findByEmail("alex@test.com")).thenReturn(Optional.of(user));

        User result = userService.findByEmail("alex@test.com");

        assertNotNull(result);
        assertEquals("alex@test.com", result.getEmail());
        verify(userRepository).findByEmail("alex@test.com");
    }

    @Test
    void shouldThrowWhenUserNotFoundByEmail() {
        when(userRepository.findByEmail("alex@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findByEmail("alex@test.com"));

        verify(userRepository).findByEmail("alex@test.com");
    }
}