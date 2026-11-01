package com.mindbridge.chat;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReportRepository extends JpaRepository<ChatReport, UUID> {
}
