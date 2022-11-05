package com.example.finalproject.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MemberRequestDto {

    @NotBlank(message = "{email.notblank}")
    @Pattern(regexp ="^[a-zA-Z0-9]+@[a-zA-Z]+.[a-z]+${4,12}$", message = "{email.option}" )
    private String email;

    @NotBlank(message = "{password.notblank}")
    private String password;

    @NotBlank(message = "{passwordConfirm.notblank}")
    private String passwordConfirm;

    @NotBlank(message = "{nickname.notblank}")
    private String nickname;
}
