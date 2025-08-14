package com.talkflow.websocket;

import com.talkflow.configuration.jwt.JWTService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {

        logger.info("Incoming WebSocket handshake: {}", request.getURI());

        String uri = request.getURI().toString();
        String token = extractTokenFromUri(uri);
        logger.info("Extracted token: {}", token);

        if (token != null) {
            String userEmail = null;
            try {
                userEmail = jwtService.extractUsername(token);
                logger.info("Extracted email from token: {}", userEmail);
            } catch (Exception e) {
                logger.error("Failed to extract username from token: {}", e.getMessage());
            }

            if (userEmail != null) {
                try {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    boolean valid = jwtService.isTokenValid(token, userDetails);
                    logger.info("Token validation result: {}", valid);

                    if (valid) {
                        attributes.put("user", userDetails);
                        attributes.put("username", userEmail);
                        logger.info("Handshake success for user: {}", userEmail);
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("Error loading user or validating token: {}", e.getMessage());
                }
            }
        }

        logger.error("Handshake rejected. Token missing/invalid.");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception == null) {
            logger.info("WebSocket handshake completed successfully for URI: {}", request.getURI());
        } else {
            logger.error("WebSocket handshake failed for URI: {} with exception: {}", request.getURI(), exception.getMessage());
        }
    }

    private String extractTokenFromUri(String uriString) {
        try {
            URI uri = new URI(uriString);
            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        return param.substring(6);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse URI for token: {}", e.getMessage());
        }
        return null;
    }
}
