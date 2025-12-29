package nl.cyberella.hands_on.controllers;

import nl.cyberella.hands_on.dto.user.ImageRequest;
import org.springframework.http.ResponseEntity;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import nl.cyberella.hands_on.services.interfaces.IUserService;

/**
 * Controller that exposes the image/entries endpoint used by the frontend to
 * increment a user's image processing counter.
 *
 * The endpoint accepts an {@link ImageRequest} body which should contain:
 * - id: the numeric id of the user to update
 * - faceCount: (optional) number of faces detected in the last image
 *
 * Behavior summary:
 * - Delegates the increment logic to the user service's incrementEntries method
 * - If the user id is unknown, returns 400 with a short error message
 * - On success returns 200 with the new entries count (Integer)
 */
@RestController
public class ImageController {
    
    // depends on IUserService interface via constructor injection
    private final IUserService userService;

    public ImageController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * PUT /image
     *
     * Request body: {@link ImageRequest}
     * - id (Integer) - required, the user id
     * - faceCount (Integer) - optional, how many faces were detected; passed
     *   through to the service so it can decide how many entries to add.
     *
     * Response:
     * - 200 OK with the updated entries count (Integer) on success
     * - 400 if the user is not found (keeps parity with the original behaviour)
     */
    @PutMapping("/image")
    public ResponseEntity<?> imageEntries(@Valid @RequestBody ImageRequest req) {
        // Delegate to the service to modify and persist the entries count.
        // Service returns the new count, or null when the user id doesn't exist.
        Integer entries = userService.incrementEntries(req.getId(), req.getFaceCount());
        if (entries == null) {
            // 400 used to match previous behaviour; consider 404 for clearer semantics.
            throw new EntityNotFoundException("user not found");
        }
        return ResponseEntity.ok(entries);
    }
}
