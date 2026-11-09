package com.tradedoc.origincheck.auth;

import com.tradedoc.origincheck.common.ApiException;
import com.tradedoc.origincheck.common.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";
    private static final String ISSUER = "fta-origin-check-api";

    private final JwtProvider provider = new JwtProvider(new JwtProperties(SECRET, 60, ISSUER));

    @Test
    void 발급한_토큰을_검증하면_사용자가_나온다() {
        AuthUser user = provider.verify(provider.issue(7L, "a@example.com"));

        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.email()).isEqualTo("a@example.com");
        assertThat(user.expiresAt()).isBetween(Instant.now().plus(Duration.ofMinutes(59)), Instant.now().plus(Duration.ofMinutes(61)));
        assertThat(provider.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void 만료된_토큰은_TOKEN_EXPIRED() {
        String token = signed(SECRET, ISSUER, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
        assertCode(token, ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void 다른_키로_서명한_토큰은_거절() {
        String token = signed("another-secret-another-secret-another-secret!!", ISSUER, Instant.now(), Instant.now().plusSeconds(600));
        assertCode(token, ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 발급자가_다르면_거절() {
        String token = signed(SECRET, "someone-else", Instant.now(), Instant.now().plusSeconds(600));
        assertCode(token, ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 서명_없는_alg_none_토큰은_거절() {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        long now = Instant.now().getEpochSecond();
        String header = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(("{\"sub\":\"1\",\"iss\":\"" + ISSUER + "\",\"iat\":" + now + ",\"exp\":" + (now + 600) + "}")
                .getBytes(StandardCharsets.UTF_8));
        assertCode(header + "." + payload + ".", ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 내용을_바꾼_토큰은_거절() {
        String[] parts = provider.issue(1L, "a@example.com").split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"2\",\"iss\":\"" + ISSUER + "\",\"iat\":1,\"exp\":9999999999}").getBytes(StandardCharsets.UTF_8));
        assertCode(parts[0] + "." + forged + "." + parts[2], ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 형식이_틀린_토큰은_거절() {
        assertCode("abc.def.ghi", ErrorCode.UNAUTHORIZED);
        assertCode("", ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 비밀_키가_없거나_짧으면_AUTH_NOT_CONFIGURED() {
        for (String secret : new String[]{null, "", "too-short"}) {
            JwtProvider p = new JwtProvider(new JwtProperties(secret, 60, ISSUER));
            assertThatThrownBy(() -> p.issue(1L, "a@example.com"))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.AUTH_NOT_CONFIGURED));
            assertThatThrownBy(() -> p.verify("x.y.z"))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.AUTH_NOT_CONFIGURED));
        }
    }

    private void assertCode(String token, ErrorCode expected) {
        assertThatThrownBy(() -> provider.verify(token))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    private static String signed(String secret, String issuer, Instant iat, Instant exp) {
        return Jwts.builder()
                .subject("1")
                .issuer(issuer)
                .issuedAt(Date.from(iat))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }
}
