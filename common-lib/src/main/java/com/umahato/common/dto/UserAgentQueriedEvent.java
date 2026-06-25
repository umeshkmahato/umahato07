package com.umahato.common.dto;

import java.time.Instant;

public record UserAgentQueriedEvent(
        String eventId,
        String eventType,
        Instant timestamp,
        String sessionId,
        Long userId,
        String query,
        String resultStatus,
        String source
) {
}
