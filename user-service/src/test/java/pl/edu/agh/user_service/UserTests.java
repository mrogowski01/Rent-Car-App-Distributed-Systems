package pl.edu.agh.user_service;

import org.junit.jupiter.api.Test;
import pl.edu.agh.user_service.payload.request.SignupRequest;
import pl.edu.agh.validators.UserValidator;

import static org.junit.jupiter.api.Assertions.*;

class UserValidatorTest {

    @Test
    void validateSignupRequest_validData_shouldReturnNull() {
        SignupRequest validRequest = new SignupRequest();
        validRequest.setUsername("valid.email@example.com");
        validRequest.setPassword("StrongPassword123");

        String result = UserValidator.validateSignupRequest(validRequest);

        assertNull(result, "Expected null for valid input");
    }

    @Test
    void validateSignupRequest_invalidEmail_shouldReturnErrorMessage() {
        SignupRequest invalidEmailRequest = new SignupRequest();
        invalidEmailRequest.setUsername("invalid-email");
        invalidEmailRequest.setPassword("ValidPassword123");

        String result = UserValidator.validateSignupRequest(invalidEmailRequest);

        assertEquals("Invalid email format.", result, "Expected invalid email error message");
    }

    @Test
    void validateSignupRequest_nullEmail_shouldReturnErrorMessage() {
        SignupRequest nullEmailRequest = new SignupRequest();
        nullEmailRequest.setUsername(null);
        nullEmailRequest.setPassword("ValidPassword123");

        String result = UserValidator.validateSignupRequest(nullEmailRequest);

        assertEquals("Invalid email format.", result, "Expected invalid email error message");
    }

    @Test
    void validateSignupRequest_shortPassword_shouldReturnErrorMessage() {
        SignupRequest shortPasswordRequest = new SignupRequest();
        shortPasswordRequest.setUsername("valid.email@example.com");
        shortPasswordRequest.setPassword("short");

        String result = UserValidator.validateSignupRequest(shortPasswordRequest);

        assertEquals("Password must be more than 8 characters.", result, "Expected password length error message");
    }

    @Test
    void validateSignupRequest_nullPassword_shouldReturnErrorMessage() {
        SignupRequest nullPasswordRequest = new SignupRequest();
        nullPasswordRequest.setUsername("valid.email@example.com");
        nullPasswordRequest.setPassword(null);

        String result = UserValidator.validateSignupRequest(nullPasswordRequest);

        assertEquals("Password must be more than 8 characters.", result, "Expected password length error message");
    }
}
