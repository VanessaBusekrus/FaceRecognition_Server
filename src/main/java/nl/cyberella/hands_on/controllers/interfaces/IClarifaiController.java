package nl.cyberella.hands_on.controllers.interfaces;

import nl.cyberella.hands_on.dto.clarifai.ClarifaiRequest;
import org.springframework.http.ResponseEntity;

public interface IClarifaiController {
    ResponseEntity<?> analyze(ClarifaiRequest body);
}
