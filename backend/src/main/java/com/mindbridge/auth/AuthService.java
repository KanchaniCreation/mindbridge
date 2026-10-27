package com.mindbridge.auth;

import com.mindbridge.common.ApiException;
import com.mindbridge.user.User;
import com.mindbridge.user.UserPreferences;
import com.mindbridge.user.UserPreferencesRepository;
import com.mindbridge.user.UserRepository;
import com.mindbridge.user.UserResponse;
import java.time.Clock;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(UserRepository users, UserPreferencesRepository preferences,
                       PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.preferences = preferences;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Creates the user and their default preferences in one transaction (both or neither). */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw emailTaken();
        }
        String displayName = request.displayName() == null || request.displayName().isBlank()
            ? null : request.displayName().trim();
        User user = new User(email, passwordEncoder.encode(request.password()), displayName, clock.instant());
        try {
            // Flush now so a concurrent duplicate registration (race) fails on the unique index here.
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw emailTaken();
        }
        preferences.save(UserPreferences.defaults(user.getId()));
        return UserResponse.from(user);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
    }
}
