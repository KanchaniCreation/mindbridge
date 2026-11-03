package com.mindbridge.chat;

import com.mindbridge.common.ApiException;
import com.mindbridge.user.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** The one place that checks "this person confirmed they are 18 or older". */
@Component
public class AdultGate {

    private final UserRepository users;

    public AdultGate(UserRepository users) {
        this.users = users;
    }

    public void requireAdult(UUID userId) {
        boolean confirmed = users.findById(userId)
            .map(u -> u.getAdultConfirmedAt() != null)
            .orElse(false);
        if (!confirmed) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                "Please confirm that you are 18 or older to use chat.");
        }
    }
}
