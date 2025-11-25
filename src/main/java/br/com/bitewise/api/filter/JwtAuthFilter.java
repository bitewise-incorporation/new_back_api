package br.com.bitewise.api.filter;

import br.com.bitewise.api.service.UserDetailsServiceImpl;
import br.com.bitewise.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT filter for health check and auth routes (public routes)
        String requestURI = request.getRequestURI();
        logger.info("JwtAuthFilter - Processing request: {}", requestURI);
        
        if (requestURI.contains("/health") || requestURI.startsWith("/api/auth/")) {
            logger.info("JwtAuthFilter - Skipping JWT check for permitAll route: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                logger.info("🔑 [JwtAuthFilter] Token encontrado: {}...", token.substring(0, Math.min(20, token.length())));
                username = jwtUtil.extractUsername(token);
                logger.info("👤 [JwtAuthFilter] Username extraído: {}", username);
            } else {
                logger.warn("⚠️ [JwtAuthFilter] Sem header Authorization ou não começa com 'Bearer '");
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.info("🔍 [JwtAuthFilter] Carregando UserDetails para: {}", username);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    logger.info("✅ [JwtAuthFilter] Token válido! Autenticando usuário: {}", username);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("🔓 [JwtAuthFilter] Autenticação definida com sucesso");
                } else {
                    logger.warn("❌ [JwtAuthFilter] Token inválido para usuário: {}", username);
                }
            } else if (username == null) {
                logger.warn("⚠️ [JwtAuthFilter] Username é null, pulando autenticação");
            }
        } catch (Exception e) {
            logger.error("❌ [JwtAuthFilter] Erro ao processar JWT: {}", e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }
}