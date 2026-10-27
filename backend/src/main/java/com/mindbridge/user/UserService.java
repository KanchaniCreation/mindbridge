package com.mindbridge.user;

import com.mindbridge.common.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every method takes the user id from the authenticated session (never from the request),
 * so a user can only ever read or change their own records.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, UserPreferencesRepository preferences,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.preferences = preferences;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setDisplayName(normalizeName(request.displayName()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(UUID userId) {
        return PreferencesResponse.from(findPreferences(userId));
    }

    @Transactional
    public PreferencesResponse updatePreferences(UUID userId, PreferencesRequest request) {
        if ((request.quietStart() == null) != (request.quietEnd() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Set both quiet hours start and end, or leave both empty.");
        }
        UserPreferences prefs = findPreferences(userId);
        prefs.update(request.preferredStyle(), request.quietStart(), request.quietEnd(),
            request.remindersEnabled());
        return PreferencesResponse.from(prefs);
    }

    /** Re-checks the password, then deletes the user. The database cascades to all owned rows. */
    @Transactional
    public void deleteAccount(UUID userId, String password) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Password confirmation failed.");
        }
        users.delete(user);
    }

    private User findUser(UUID userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    private UserPreferences findPreferences(UUID userId) {
        return preferences.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Preferences not found."));
    }

    static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }
}
