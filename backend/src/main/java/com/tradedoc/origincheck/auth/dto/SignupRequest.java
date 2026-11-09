package com.tradedoc.origincheck.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입. 화면명세서 M-02 기준.
 *
 * @param company 선택
 */
public record SignupRequest(
        @NotBlank(message = "이메일을 입력하세요.") @Email(message = "이메일 형식이 아닙니다.") @Size(max = 255) String email,
        @NotBlank(message = "비밀번호를 입력하세요.") @Size(min = 8, max = 200, message = "비밀번호는 8자 이상이어야 합니다.") String password,
        @NotBlank(message = "이름을 입력하세요.") @Size(max = 50, message = "이름은 50자 이하여야 합니다.") String name,
        @Size(max = 100, message = "회사명은 100자 이하여야 합니다.") String company
) {
}
