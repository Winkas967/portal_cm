package com.portal_cm.portal_cm.users.auth.dto;

import java.util.List;

public record MeResponse(
        String username,
        List<String> role,
        String displayRole
) {
}
