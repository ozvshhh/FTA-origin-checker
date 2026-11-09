package com.tradedoc.origincheck.auth;

import com.tradedoc.origincheck.common.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer 토큰을 확인해 SecurityContext 에 AuthUser 를 넣는다.
 * 토큰이 틀리면 오류 코드를 요청 속성에 남기고 그대로 넘긴다 → 보호된 경로면 {@link JsonAuthenticationEntryPoint} 가 401 로 응답.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                AuthUser user = jwtProvider.verify(header.substring(7).trim());
                var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ApiException e) {
                request.setAttribute(ERROR_ATTRIBUTE, e);
            }
        }
        chain.doFilter(request, response);
    }
}
