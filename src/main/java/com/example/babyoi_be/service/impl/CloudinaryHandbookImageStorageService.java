package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.domain.dto.respone.HandbookImageResponse;
import com.example.babyoi_be.service.HandbookImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryHandbookImageStorageService implements HandbookImageStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final WebClient.Builder webClientBuilder;
    private final CloudinaryTimestampService cloudinaryTimestampService;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${app.cloudinary.handbook-folder:camnang}")
    private String handbookFolder;

    @Override
    public String uploadImage(MultipartFile file) {
        validateFile(file);
        validateCloudinaryConfig();

        try {
            String targetFolder = resolveFolder();
            long timestamp = cloudinaryTimestampService.currentEpochSecond(cloudName);
            Map<String, String> signedParams = new LinkedHashMap<>();
            signedParams.put("asset_folder", targetFolder);
            signedParams.put("folder", targetFolder);
            signedParams.put("overwrite", "true");
            signedParams.put("public_id", "handbook_" + UUID.randomUUID());
            signedParams.put("timestamp", String.valueOf(timestamp));

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new NamedByteArrayResource(file.getBytes(), sanitizeFileName(file.getOriginalFilename())))
                    .contentType(resolveContentType(file));
            builder.part("api_key", apiKey);
            signedParams.forEach(builder::part);
            builder.part("signature", createSignature(signedParams));

            MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = builder.build();
            Map<?, ?> response = webClientBuilder.build()
                    .post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/upload", cloudName)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            cloudinaryResponse -> cloudinaryResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(errorBody -> {
                                        log.warn("Cloudinary handbook upload failed with status {} and body {}", cloudinaryResponse.statusCode(), errorBody);
                                        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary không nhận ảnh cẩm nang");
                                    })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            Object secureUrl = response != null ? response.get("secure_url") : null;
            if (secureUrl instanceof String url && !url.isBlank()) {
                log.info("Handbook image uploaded to Cloudinary folder {}", targetFolder);
                return url;
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không nhận được URL ảnh cẩm nang từ Cloudinary");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Handbook image upload failed", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể tải ảnh cẩm nang lên Cloudinary", exception);
        }
    }

    @Override
    public List<HandbookImageResponse> listImages() {
        validateCloudinaryConfig();
        try {
            Map<?, ?> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.cloudinary.com")
                            .path("/v1_1/{cloudName}/resources/image/upload")
                            .queryParam("prefix", resolveFolder() + "/")
                            .queryParam("max_results", 100)
                            .build(cloudName))
                    .headers(headers -> headers.setBasicAuth(apiKey, apiSecret))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            Object resources = response != null ? response.get("resources") : null;
            if (!(resources instanceof List<?> items)) {
                return List.of();
            }

            return items.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(item -> new HandbookImageResponse(
                            String.valueOf(item.get("public_id")),
                            String.valueOf(item.get("secure_url"))))
                    .filter(item -> !item.imageUrl().isBlank() && !"null".equals(item.imageUrl()))
                    .toList();
        } catch (Exception exception) {
            log.warn("Could not list handbook images from Cloudinary", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể lấy thư viện ảnh cẩm nang", exception);
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null || !publicId.startsWith(resolveFolder() + "/")) {
            return;
        }

        validateCloudinaryConfig();
        try {
            long timestamp = cloudinaryTimestampService.currentEpochSecond(cloudName);
            Map<String, String> signedParams = new LinkedHashMap<>();
            signedParams.put("public_id", publicId);
            signedParams.put("timestamp", String.valueOf(timestamp));

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("api_key", apiKey);
            signedParams.forEach(builder::part);
            builder.part("signature", createSignature(signedParams));

            webClientBuilder.build()
                    .post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/destroy", cloudName)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception exception) {
            log.warn("Could not delete handbook image from Cloudinary: {}", publicId, exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh cẩm nang");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh cẩm nang không được vượt quá 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tải lên phải là ảnh");
        }
    }

    private void validateCloudinaryConfig() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chưa cấu hình Cloudinary cho ảnh cẩm nang");
        }
    }

    private String resolveFolder() {
        return handbookFolder == null || handbookFolder.isBlank() ? "camnang" : handbookFolder.trim();
    }

    private String createSignature(Map<String, String> params) {
        String payload = new TreeMap<>(params).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&")) + apiSecret;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể ký yêu cầu Cloudinary", exception);
        }
    }

    private MediaType resolveContentType(MultipartFile file) {
        try {
            return file.getContentType() != null ? MediaType.parseMediaType(file.getContentType()) : MediaType.IMAGE_JPEG;
        } catch (Exception exception) {
            return MediaType.IMAGE_JPEG;
        }
    }

    private String sanitizeFileName(String originalFilename) {
        return originalFilename == null || originalFilename.isBlank()
                ? "handbook.jpg"
                : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String marker = "res.cloudinary.com/" + cloudName + "/image/upload/";
        int markerIndex = imageUrl.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        String[] parts = imageUrl.substring(markerIndex + marker.length()).split("/");
        int start = 0;
        while (start < parts.length && (parts[start].matches("v\\d+") || parts[start].contains(","))) {
            start++;
        }
        if (start >= parts.length) {
            return null;
        }
        String value = String.join("/", Arrays.copyOfRange(parts, start, parts.length));
        int extensionIndex = value.lastIndexOf('.');
        String publicId = extensionIndex > 0 ? value.substring(0, extensionIndex) : value;
        try {
            return URLDecoder.decode(publicId, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return publicId;
        }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
