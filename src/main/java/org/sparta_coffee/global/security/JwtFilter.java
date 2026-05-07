package org.sparta_coffee.global.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sparta_coffee.global.dto.ErrorResponse;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (!StringUtils.hasText(token)) {
                if (isPermitAllRequest(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                sendErrorResponse(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            jwtTokenProvider.validateTokenOrThrow(token);

            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            sendErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
        } catch (MalformedJwtException exception) {
            sendErrorResponse(response, ErrorCode.TOKEN_MALFORMED);
        } catch (SignatureException exception) {
            sendErrorResponse(response, ErrorCode.TOKEN_SIGNATURE_INVALID);
        } catch (IllegalArgumentException exception) {
            sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private void sendErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse<Void> errorResponse = ErrorResponse.of(
                errorCode.getCode(),
                errorCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }


    // 공개 API는 제외.
    private boolean isPermitAllRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        return ("POST".equals(method) && "/api/v1/users".equals(path))
                || ("POST".equals(method) && "/api/v1/auth/login".equals(path))
                || ("GET".equals(method) && "/api/v1/popular-ranking/today".equals(path));
    }
}
