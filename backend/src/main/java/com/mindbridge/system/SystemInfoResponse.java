package com.mindbridge.system;

import java.time.Instant;

public record SystemInfoResponse(String name, String version, String status, Instant serverTime) {}
