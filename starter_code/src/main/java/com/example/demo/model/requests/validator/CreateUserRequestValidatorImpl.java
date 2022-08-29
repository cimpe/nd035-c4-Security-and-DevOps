package com.example.demo.model.requests.validator;

import com.example.demo.model.requests.CreateUserRequest;
import org.springframework.stereotype.Component;

@Component
public final class CreateUserRequestValidatorImpl implements RequestValidator<CreateUserRequest> {

    @Override
    public boolean isRequestValid(final CreateUserRequest req) {
        if (req.getPassword().length() < 7) {
            return false;
        }

        if (!req.getPassword().equals(req.getConfirmPassword())) {
            return false;
        }

        return true;
    }

}
