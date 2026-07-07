package com.ashutosh.cyberhelper.dto.user;

import java.util.List;

public record AssignRolesRequest(
        List<Long> roleIds
) {
}
