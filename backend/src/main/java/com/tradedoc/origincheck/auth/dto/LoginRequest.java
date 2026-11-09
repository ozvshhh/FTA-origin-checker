package com.tradedoc.origincheck.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "이메일을 입력하세요.") @Email(message = "이메일 형식이 아닙니다.") @Size(max = 255) String email,
        @NotBlank(message = "비밀번호를 입력하세요.") @Size(max = 200) String password
) {
}
