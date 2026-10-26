package com.mindbridge.system;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemInfoController {

    private final String appName;
    private final String version;

    public SystemInfoController(@Value("${spring.application.name}") String appName,
                                @Value("${mindbridge.version}") String version) {
        this.appName = appName;
        this.version = version;
    }

    @GetMapping("/info")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(appName, version, "UP", Instant.now());
    }
}
