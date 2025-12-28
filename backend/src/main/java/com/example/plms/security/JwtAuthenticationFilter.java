package com.example.plms.security;

import com.example.plms.domain.AppUser;
import com.example.plms.repository.AppUserRepository;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<Long> userId = jwtService.parseUserId(token);
            if (userId.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                AppUser user = userRepository.findById(userId.get()).orElse(null);
                if (user != null) {
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(user.getId(), user.getEmail()),
                        null,
                        java.util.List.of()
                    );
                    ((UsernamePasswordAuthenticationToken) auth)
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
