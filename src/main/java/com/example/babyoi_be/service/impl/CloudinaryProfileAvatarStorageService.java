package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.service.ProfileAvatarStorageService;
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

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryProfileAvatarStorageService implements ProfileAvatarStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final WebClient.Builder webClientBuilder;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${app.cloudinary.user-avatar-folder:userAvatar}")
    private String userAvatarFolder;

    @Override
    public String uploadAvatar(MultipartFile file) {
        validateFile(file);
        validateCloudinaryConfig();

        try {
            long timestamp = Instant.now().getEpochSecond();
            String publicId = "profile_" + UUID.randomUUID();
            Map<String, String> signedParams = new LinkedHashMap<>();
            signedParams.put("folder", userAvatarFolder);
            signedParams.put("overwrite", "true");
            signedParams.put("public_id", publicId);
            signedParams.put("timestamp", String.valueOf(timestamp));
            String signature = createSignature(signedParams);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new NamedByteArrayResource(file.getBytes(), sanitizeFileName(file.getOriginalFilename())))
                    .contentType(resolveContentType(file));
            builder.part("api_key", apiKey);
            signedParams.forEach(builder::part);
            builder.part("signature", signature);

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
                                        log.warn("Cloudinary avatar upload failed with status {} and body {}", cloudinaryResponse.statusCode(), errorBody);
                                        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary không nhận ảnh hồ sơ");
                                    })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            Object secureUrl = response != null ? response.get("secure_url") : null;
            if (secureUrl instanceof String url && !url.isBlank()) {
                log.info("Profile avatar uploaded to Cloudinary folder {}", userAvatarFolder);
                return url;
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không nhận được URL ảnh từ Cloudinary");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Profile avatar upload failed", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể tải ảnh hồ sơ lên Cloudinary", exception);
        }
    }

    @Override
    public void deleteAvatar(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            return;
        }

        validateCloudinaryConfig();

        try {
            long timestamp = Instant.now().getEpochSecond();
            Map<String, String> signedParams = new LinkedHashMap<>();
            signedParams.put("public_id", publicId);
            signedParams.put("timestamp", String.valueOf(timestamp));
            String signature = createSignature(signedParams);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("api_key", apiKey);
            signedParams.forEach(builder::part);
            builder.part("signature", signature);

            webClientBuilder.build()
                    .post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/destroy", cloudName)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnNext(response -> log.info("Deleted old profile avatar {} from Cloudinary with result {}", publicId, response.get("result")))
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception exception) {
            log.warn("Could not delete old profile avatar from Cloudinary: {}", publicId, exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh hồ sơ");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh hồ sơ không được vượt quá 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tải lên phải là ảnh");
        }
    }

    private void validateCloudinaryConfig() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chưa cấu hình Cloudinary cho ảnh hồ sơ");
        }
    }

    private String createSignature(Map<String, String> params) {
        String payload = new TreeMap<>(params).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"))
                + apiSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể ký yêu cầu Cloudinary", exception);
        }
    }

    private MediaType resolveContentType(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
        } catch (Exception exception) {
            return MediaType.IMAGE_JPEG;
        }
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "avatar.jpg";
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String cloudinaryPrefix = "res.cloudinary.com/" + cloudName + "/image/upload/";
        int prefixIndex = imageUrl.indexOf(cloudinaryPrefix);
        if (prefixIndex < 0) {
            return null;
        }

        String uploadPath = imageUrl.substring(prefixIndex + cloudinaryPrefix.length());
        String[] pathParts = uploadPath.split("/");
        int publicIdStartIndex = 0;

        if (pathParts.length > 0 && pathParts[0].matches("v\\d+")) {
            publicIdStartIndex = 1;
        }

        if (publicIdStartIndex >= pathParts.length) {
            return null;
        }

        String publicIdWithExtension = String.join("/", java.util.Arrays.copyOfRange(pathParts, publicIdStartIndex, pathParts.length));
        int extensionIndex = publicIdWithExtension.lastIndexOf('.');
        String publicId = extensionIndex > 0 ? publicIdWithExtension.substring(0, extensionIndex) : publicIdWithExtension;

        try {
            return URLDecoder.decode(publicId, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return publicId;
        }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
