package com.example.demo.model.requests.validator;

import com.example.demo.model.requests.CreateUserRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CreateUserRequestValidatorImplTest {

    private static CreateUserRequestValidatorImpl validator;

    @BeforeClass
    public static void setup() {
        validator = new CreateUserRequestValidatorImpl();
    }

    @Test
    public void validatorShouldReturnTrueIfPasswordSatisfyRequirements() {
        final CreateUserRequest req = CreateUserRequest.builder()
                .password("abcdefg")
                .confirmPassword("abcdefg")
                .build();

        assertTrue(validator.isRequestValid(req));
    }

    @Test
    public void validatorShouldReturnFalseIfPasswordIsShorterThan7Characters() {
        final CreateUserRequest req = CreateUserRequest.builder()
                .password("abcdef")
                .confirmPassword("abcdef")
                .build();

        assertFalse(validator.isRequestValid(req));
    }

    @Test
    public void validatorShouldReturnFalseIfPasswordAndConfirmPasswordAreDifferent() {
        final CreateUserRequest req = CreateUserRequest.builder()
                .password("abcdefg")
                .confirmPassword("abcdefh")
                .build();

        assertFalse(validator.isRequestValid(req));
    }

}
