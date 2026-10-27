package com.mindbridge.auth;

import com.mindbridge.common.ApiException;
import com.mindbridge.security.AppUserDetails;
import com.mindbridge.user.UserResponse;
import com.mindbridge.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthService authService, UserService userService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          LoginRateLimiter rateLimiter) {
        this.authService = authService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.rateLimiter = rateLimiter;
    }

    /** Gives the SPA a CSRF cookie before its first POST. The cookie is set by the security filter. */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf() {
        // intentionally empty
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = AuthService.normalizeEmail(request.email());
        String limiterKey = email + "|" + httpRequest.getRemoteAddr();

        if (rateLimiter.isBlocked(limiterKey)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many failed attempts. Please wait a few minutes and try again.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException ex) {
            rateLimiter.recordFailure(limiterKey);
            throw ex;
        }
        rateLimiter.reset(limiterKey);

        establishSession(authentication, httpRequest, httpResponse);
        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        return userService.getProfile(principal.getId());
    }

    private void establishSession(Authentication authentication, HttpServletRequest request,
                                  HttpServletResponse response) {
        // New session id on login prevents session fixation.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.getContextHolderStrategy().setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    // Logout is handled by Spring Security's logout filter: POST /api/v1/auth/logout
}
