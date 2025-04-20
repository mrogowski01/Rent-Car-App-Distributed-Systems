package pl.edu.agh.car_service.Enums;

public enum RoleType {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_USER("ROLE_USER");

    private final String roleName;

    RoleType(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public String toString() { return roleName; }
}
