package com.tradedoc.origincheck.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SignupPropertiesTest {

    @Test
    void 비워_두면_누구나_가입() {
        assertThat(new SignupProperties(null).allows("anyone@x.com")).isTrue();
        assertThat(new SignupProperties(List.of(" ", "")).allows("anyone@x.com")).isTrue();
    }

    @Test
    void 주소와_도메인으로_허용() {
        var p = new SignupProperties(List.of(" Kim@Acme.com ", "@team.co.kr"));

        assertThat(p.allows("kim@acme.com")).isTrue();
        assertThat(p.allows("lee@team.co.kr")).isTrue();
        assertThat(p.allows("lee@acme.com")).isFalse();
        assertThat(p.allows("x@evil-team.co.kr")).isFalse();   // 도메인은 '@' 부터 통째로 같아야 한다
        assertThat(p.allows("x@sub.team.co.kr")).isFalse();
        assertThat(p.allows("team.co.kr")).isFalse();
    }
}
