package com.tradedoc.origincheck.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

/**
 * app.signup.allowed-emails — 가입을 허용할 이메일. 쉼표로 여러 개.
 * {@code kim@acme.com} 은 그 주소만, {@code @acme.com} 은 그 도메인 전체를 허용한다.
 * 비워 두면 누구나 가입할 수 있다(로컬 개발용). 공개 서버에서는 반드시 채운다.
 */
@ConfigurationProperties(prefix = "app.signup")
public record SignupProperties(List<String> allowedEmails) {

    public SignupProperties {
        allowedEmails = allowedEmails == null ? List.of() : allowedEmails.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** @param email 소문자로 정규화한 이메일 */
    public boolean allows(String email) {
        if (allowedEmails.isEmpty()) {
            return true;
        }
        int at = email.lastIndexOf('@');
        String domain = at < 0 ? null : email.substring(at);
        return allowedEmails.stream().anyMatch(a -> a.equals(email) || (a.startsWith("@") && a.equals(domain)));
    }
}
