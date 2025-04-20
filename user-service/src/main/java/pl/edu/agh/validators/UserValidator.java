package pl.edu.agh.validators;

import pl.edu.agh.user_service.payload.request.SignupRequest;

import java.util.regex.Pattern;

public class UserValidator {
    public static String validateSignupRequest(SignupRequest signUpRequest) {
        String email = signUpRequest.getUsername();
        String password = signUpRequest.getPassword();

        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        Pattern emailPattern = Pattern.compile(emailRegex);

        if (email == null || !emailPattern.matcher(email).matches()) {
            return "Invalid email format.";
        }
        if (password == null || password.length() <= 8) {
            return "Password must be more than 8 characters.";
        }

        return null;
    }
}
