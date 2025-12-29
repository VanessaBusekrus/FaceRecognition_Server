package nl.cyberella.hands_on.controllers.interfaces;

import nl.cyberella.hands_on.dto.auth.UserIdRequest;
import nl.cyberella.hands_on.dto.auth.VerifyRequest;
import org.springframework.http.ResponseEntity;

public interface ITwoFaController {
    ResponseEntity<?> enable(UserIdRequest body) throws Exception;
    ResponseEntity<?> verifySetup(VerifyRequest body);
    ResponseEntity<?> verify(VerifyRequest body);
}
