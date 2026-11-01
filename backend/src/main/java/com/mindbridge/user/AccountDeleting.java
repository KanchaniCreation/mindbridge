package com.mindbridge.user;

import java.util.UUID;

/** Published just before a user row is deleted, so other modules can tidy up their own data. */
public record AccountDeleting(UUID userId) {
}
