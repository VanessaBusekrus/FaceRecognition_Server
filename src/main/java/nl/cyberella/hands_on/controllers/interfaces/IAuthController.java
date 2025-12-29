/*
 * Interface defines the controller contract for authentication. 
 * Interfaces contain method signatures (no implementation). Used for type safety, 
 * decoupling, easier testing and clearer contracts.
 * This decouples the controller API from the implementation -> implementations can be swapped or mocked in tests
 * 
 * AuthController implements IAuthController and must provide the concrete implementations 
 * of the four methods declared below.
 */

package nl.cyberella.hands_on.controllers.interfaces;

import nl.cyberella.hands_on.dto.auth.RegisterRequest;
import nl.cyberella.hands_on.dto.auth.SigninRequest;
import nl.cyberella.hands_on.models.UpdateProfileRequest;
import org.springframework.http.ResponseEntity;

public interface IAuthController {
    // Method signatures:
    ResponseEntity<?> register(RegisterRequest body); // contract for registration endpoint. Accepts a RegisterRequest object and returns an HTTP response (wrapped as ResponseEntity<?> -> the wildcard means any body type may be returned)
    ResponseEntity<?> signin(SigninRequest body); // contract for signin endpoint. 
    ResponseEntity<?> getProfile(Integer id); // contract for fetching a profile by ID. Accepts an int (ID) and returns the object (also wildcard)
    ResponseEntity<?> updateProfile(UpdateProfileRequest req); // contract for updating a user profile. Accepts a an UpdateProfileRequest and returns an HTTP response
}
