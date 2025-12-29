package nl.cyberella.hands_on.dto.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/*
 * ImageRequest is a simple DTO for image-related requests.
 * Uses Lombok to reduce boilerplate.
 * Uses validation annotations to enforce:
 * - id is required
 * - faceCount is non-negative
 *
 * Serves as a typed object for Spring controllers to safely handle image requests
 */

@Data // generates getters, setters and other utility methods for all fields
@NoArgsConstructor // generates a no-argument constructor => new ImageRequest();
public class ImageRequest {
    @NotNull(message = "id required")
    private Integer id;

    @Min(value = 0, message = "faceCount must be >= 0")
    private Integer faceCount = 1;
}
