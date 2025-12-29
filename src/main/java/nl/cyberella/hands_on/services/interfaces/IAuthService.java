package nl.cyberella.hands_on.services.interfaces;

import nl.cyberella.hands_on.dto.auth.SigninResult;
import nl.cyberella.hands_on.models.User;

/*
IAuthService is a service interface for authentication.
Defines two main responsibilities:
- register → create a new user
- signin → authenticate a user and handle 2FA
Separates interface (contract) from implementation, making the code more modular and testable.
*/

public interface IAuthService {
    User register(String name, String email, String password);
    SigninResult signin(String email, String password);
}
