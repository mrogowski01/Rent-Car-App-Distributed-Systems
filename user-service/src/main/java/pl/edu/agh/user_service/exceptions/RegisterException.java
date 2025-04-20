package pl.edu.agh.user_service.exceptions;

public class RegisterException  extends RuntimeException {
    public RegisterException() {
        super("Couldn't register new user!");
    }
}
