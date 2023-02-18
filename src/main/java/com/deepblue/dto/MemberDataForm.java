package com.deepblue.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class MemberDataForm {

    @NotBlank(message = "회원명을 입력해야합니다")
    @Pattern(regexp = "[a-zA-Z0-9]*$", message = "영문과 숫자만 입력해야합니다")
    private String username;
    @NotBlank(message = "비밀번호를 입력해야합니다")
    private String password;
}
