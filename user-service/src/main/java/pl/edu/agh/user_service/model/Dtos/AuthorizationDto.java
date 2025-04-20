package pl.edu.agh.user_service.model.Dtos;

import pl.edu.agh.user_service.enums.RoleType;

public record AuthorizationDto (
        String token,
        RoleType roleType
) {}