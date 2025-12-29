package nl.cyberella.hands_on.dto.clarifai;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for Clarifai analyze endpoint.
 */
public record ClarifaiRequest(@NotBlank(message = "url required") String url) {
}
