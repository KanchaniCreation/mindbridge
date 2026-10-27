package com.mindbridge.user;

import com.mindbridge.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return userService.getProfile(principal.getId());
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal AppUserDetails principal,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getId(), request);
    }

    @GetMapping("/preferences")
    public PreferencesResponse preferences(@AuthenticationPrincipal AppUserDetails principal) {
        return userService.getPreferences(principal.getId());
    }

    @PutMapping("/preferences")
    public PreferencesResponse updatePreferences(@AuthenticationPrincipal AppUserDetails principal,
                                                 @Valid @RequestBody PreferencesRequest request) {
        return userService.updatePreferences(principal.getId(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AppUserDetails principal,
                                              @Valid @RequestBody DeleteAccountRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        userService.deleteAccount(principal.getId(), request.password());
        new SecurityContextLogoutHandler().logout(
            httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
    }
}
