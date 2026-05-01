package com.group11.bugreporter.dto.response;

import com.group11.bugreporter.entity.enums.Role;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse{
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean banned;
    private LocalDateTime createdAt;
    //am adaugat asta aici pentru ca imi trebuie la banning (trmit mesaj pe sms dupa ce un user e banned)
    private String phoneNumber;

}