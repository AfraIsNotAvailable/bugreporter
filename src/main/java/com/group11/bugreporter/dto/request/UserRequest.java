package com.group11.bugreporter.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest{
    private String username;
    private String password;
    private String email;
    //am adaugat asta aici pentru ca imi trebuie la banning (trmit mesaj pe sms dupa ce un user e banned)
    private String phoneNumber;
}