package pl.edu.agh.car_service.Models.Authorization;

import pl.edu.agh.car_service.Enums.RoleType;

public record AuthorizationDto (
        String token,
        RoleType roleType
) {}
