package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.UserRequest;
import com.group11.bugreporter.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import com.group11.bugreporter.entity.User;


public interface UserService{
    UserResponse createUser(UserRequest dto);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, UserRequest dto);
    void deleteUser(Long id);

    //pentru operatiile legate de cont
    UserResponse banUser(Long id);
    UserResponse unbanUser(Long id);

    //asta poate trebuie la autentificare
    User findByEmail(String email);

}