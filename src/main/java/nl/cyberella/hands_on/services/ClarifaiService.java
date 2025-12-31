package nl.cyberella.hands_on.services;

import com.clarifai.channel.ClarifaiChannel;
import com.clarifai.credentials.ClarifaiCallCredentials;
import com.clarifai.grpc.api.*;
import com.clarifai.grpc.api.status.StatusCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import nl.cyberella.hands_on.services.interfaces.IClarifaiService;

import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

// @Service annotation so it can be injected into other services/controllers
@Service
@Slf4j
public class ClarifaiService implements IClarifaiService {

    @Value("${clarifai.api.pat:}")
    private String PAT;
    @Value("${clarifai.api.user-id:}")
    private String USER_ID;
    @Value("${clarifai.api.app-id:}")
    private String APP_ID;

    private static final String MODEL_ID = "face-detection";
    private static final String MODEL_VERSION_ID = "6dc7e46bc9124c5c8824be4822abe105";

    // Reused channel and stub for the lifetime of this service, instead of reconnecting every time
    // ManagedChannel → the connection to Clarifai’s gRPC server.
    // V2BlockingStub → client that actually sends requests.
    private ManagedChannel channel;
    private V2Grpc.V2BlockingStub stub;

    // Runs after Spring constructs the bean
    @PostConstruct
    public void init() {
        try {
            if (!StringUtils.hasText(PAT)) {
                log.warn("Clarifai PAT not configured; Clarifai client will be disabled");
                return;
            }
            // Creates a gRPC channel and stub with authentication
            this.channel = ClarifaiChannel.INSTANCE.getGrpcChannel();
            this.stub = V2Grpc.newBlockingStub(this.channel)
            // ClarifaiCallCredentials → attaches the PAT to every gRPC call.
                    .withCallCredentials(new ClarifaiCallCredentials(PAT));
            log.info("Clarifai gRPC client initialized");
        } catch (Exception ex) {
            log.error("Failed to initialize Clarifai client", ex);
            // leave stub null so analyzeUrl throws a helpful error
        }
    }

    // Face detection
    public Map<String, Object> analyzeUrl(String url) throws Exception {
        if (stub == null) throw new IllegalStateException("Clarifai client not initialized or PAT not configured");

        // Telling Clarifai all the information it needs
        PostModelOutputsRequest request = PostModelOutputsRequest.newBuilder()
                .setUserAppId(UserAppIDSet.newBuilder().setUserId(USER_ID).setAppId(APP_ID))
                .setModelId(MODEL_ID)
                .setVersionId(MODEL_VERSION_ID)
                .addInputs(Input.newBuilder().setData(
                        Data.newBuilder().setImage(Image.newBuilder().setUrl(url))
                ))
                .build();

    // Sending the request and checking the status
    MultiOutputResponse response = stub.postModelOutputs(request);

    var status = response.getStatus();
    if (status.getCode() != StatusCode.SUCCESS) {
        throw new RuntimeException("Clarifai gRPC call failed: " + status);
    }

        // Map results: create a list of detected regions with bounding boxes.
        // Be defensive: iterate all outputs and skip malformed regions instead of throwing.
        List<Map<String, Object>> regionsOut = new ArrayList<>();
        if (response.getOutputsCount() > 0) {
            for (Output output : response.getOutputsList()) {
                if (output == null) continue;
                var data = output.getData();
                if (data == null) continue;
                for (Region region : data.getRegionsList()) {
                    try {
                        if (region == null) continue;
                        var regionInfoObj = region.getRegionInfo();
                        if (regionInfoObj == null) continue;
                        var box = regionInfoObj.getBoundingBox();
                        if (box == null) continue;

                        Map<String, Object> bbox = new java.util.LinkedHashMap<>();
                        bbox.put("top_row", box.getTopRow());
                        bbox.put("left_col", box.getLeftCol());
                        bbox.put("bottom_row", box.getBottomRow());
                        bbox.put("right_col", box.getRightCol());

                        Map<String, Object> regionInfo = new java.util.LinkedHashMap<>();
                        regionInfo.put("bounding_box", bbox);

                        Map<String, Object> regionMap = new java.util.LinkedHashMap<>();
                        regionMap.put("region_info", regionInfo);

                        regionsOut.add(regionMap);
                    } catch (Exception ex) {
                        // skip this region if any unexpected structure is encountered
                        log.warn("Skipping malformed Clarifai region: {}", ex.getMessage());
                    }
                }
            }
        }

        // Also expose a simple top-level list of bounding boxes (one per detected face)
        List<Map<String, Object>> bboxList = new ArrayList<>();
        for (Map<String, Object> regionMap : regionsOut) {
            try {
                Object regionInfoObj = regionMap.get("region_info");
                if (!(regionInfoObj instanceof Map)) continue;
                Map<?,?> regionInfo = (Map<?,?>) regionInfoObj;
                Object bboxObj = regionInfo.get("bounding_box");
                if (!(bboxObj instanceof Map)) continue;
                Map<?,?> bboxMap = (Map<?,?>) bboxObj;
                Map<String, Object> simple = new java.util.LinkedHashMap<>();
                simple.put("top_row", bboxMap.get("top_row"));
                simple.put("left_col", bboxMap.get("left_col"));
                simple.put("bottom_row", bboxMap.get("bottom_row"));
                simple.put("right_col", bboxMap.get("right_col"));
                bboxList.add(simple);
            } catch (Exception ex) {
                // ignore malformed region entry
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("outputs", List.of(Map.of("data", Map.of("regions", regionsOut))));
        out.put("regions", bboxList);
        return out;
    }

    // Runs when the service is destroyed (e.g., server shutdown).
    // Closes the gRPC channel gracefully.
    // Avoids leaking threads or connections.
    @PreDestroy
    public void shutdown() {
        if (this.channel != null) {
            try {
                this.channel.shutdown();
                if (!this.channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.channel.shutdownNow();
            } catch (Exception e) {
                log.warn("Exception while shutting down Clarifai channel", e);
            }
        }
    }
}
