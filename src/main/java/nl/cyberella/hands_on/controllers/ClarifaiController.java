package nl.cyberella.hands_on.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;
import nl.cyberella.hands_on.services.interfaces.IClarifaiService;
import nl.cyberella.hands_on.dto.clarifai.ClarifaiRequest;
import nl.cyberella.hands_on.controllers.interfaces.IClarifaiController;

@RestController
@Slf4j
public class ClarifaiController implements IClarifaiController {

    // Service that this controller depends on
    private final IClarifaiService clarifaiService;

    public ClarifaiController(IClarifaiService clarifaiService) {
        this.clarifaiService = clarifaiService;
    }

    @PostMapping("/clarifaiAPI")
    public ResponseEntity<?> analyze(@Valid @RequestBody ClarifaiRequest body) {
    var url = body.url();

        try {
            Map<String, Object> result = clarifaiService.analyzeUrl(url);
            return ResponseEntity.ok(result);

        } catch (IllegalStateException ex) {
            // For application-level validation issues -> 400
            throw new IllegalArgumentException(ex.getMessage());

        } catch (Exception ex) {
            log.error("Clarifai analyze failed for url={}: {}", url, ex.getMessage());
            // Handle Clarifai-specific "download failed" errors gracefully
            if (ex.getMessage() != null && ex.getMessage().contains("INPUT_DOWNLOAD_FAILED")) {
                throw new IllegalArgumentException("This image host blocks external downloads. Please upload the image or use a different URL.");
            }

            // Generic fallback for any other unexpected Clarifai or server errors -> let GlobalExceptionHandler produce 500
            throw new RuntimeException("clarifai error");
        }
    }

}
