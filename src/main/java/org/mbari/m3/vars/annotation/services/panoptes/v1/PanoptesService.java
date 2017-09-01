package org.mbari.m3.vars.annotation.services.panoptes.v1;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.mbari.m3.vars.annotation.model.ImageUploadResults;
import org.mbari.m3.vars.annotation.services.AuthService;
import org.mbari.m3.vars.annotation.services.ImageArchiveService;
import org.mbari.m3.vars.annotation.services.RetrofitWebService;
import retrofit2.http.Path;

import javax.inject.Named;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Brian Schlining
 * @since 2017-08-31T14:03:00
 */
public class PanoptesService implements ImageArchiveService, RetrofitWebService {

    private final PanoptesWebService webService;
    private final Map<String, String> defaultHeaders;

    public PanoptesService(PanoptesWebServiceFactory serviceFactory,
                           @Named("PANOPTES_AUTH") AuthService authService) {
        webService = serviceFactory.create(PanoptesWebService.class, authService);
        defaultHeaders = new HashMap<>();
        defaultHeaders.put("Accept", "application/json");
        defaultHeaders.put("Accept-Charset", "utf-8");
    }

    @Override
    public CompletableFuture<ImageUploadResults> upload(String cameraId, String deploymentId, String name, java.nio.file.Path image) {
        RequestBody requestBody = RequestBody.create(MultipartBody.FORM, image.toFile());
        MultipartBody.Part body = MultipartBody.Part.createFormData("file",
                image.getFileName().toString(),
                requestBody);
        return sendRequest(webService.uploadImage(cameraId, deploymentId, name, body,
                requestBody, defaultHeaders));
    }

    @Override
    public CompletableFuture<ImageUploadResults> locate(String cameraId, String deploymentId, String name) {
        return sendRequest(webService.findImage(cameraId, deploymentId, name));
    }
}
