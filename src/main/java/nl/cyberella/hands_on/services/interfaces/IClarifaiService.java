package nl.cyberella.hands_on.services.interfaces;

import java.util.Map;

/*
IClarifaiService is a service interface for interacting with the Clarifai API.
Defines a single method, analyzeUrl, which takes an image URL and returns a map of results.
Implementation (like ClarifaiService) handles:
- gRPC connection
- Model selection
- Parsing response
- Returning structured results
*/

public interface IClarifaiService {
    Map<String, Object> analyzeUrl(String url) throws Exception;
}
