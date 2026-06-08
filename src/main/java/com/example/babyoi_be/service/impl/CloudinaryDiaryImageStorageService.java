package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.service.DiaryImageStorageService;
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
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryDiaryImageStorageService implements DiaryImageStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final WebClient.Builder webClientBuilder;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${app.cloudinary.diary-folder:Diary}")
    private String diaryFolder;

    @Value("${app.cloudinary.diary-upload-timeout-seconds:45}")
    private long diaryUploadTimeoutSeconds;

    @Value("${app.cloudinary.diary-image-max-width:1080}")
    private int diaryImageMaxWidth;

    @Value("${app.cloudinary.diary-image-max-height:1080}")
    private int diaryImageMaxHeight;

    @Value("${app.cloudinary.diary-image-quality:0.72}")
    private float diaryImageQuality;

    @Value("${app.cloudinary.diary-compression-min-bytes:180000}")
    private long diaryCompressionMinBytes;

    @Override
    public String uploadImage(MultipartFile file) {
        validateFile(file);
        validateCloudinaryConfig();

        try {
            UploadImageContent uploadContent = prepareUploadContent(file);
            String targetFolder = resolveDiaryFolder();
            long timestamp = Instant.now().getEpochSecond();
            String publicId = "diary_" + UUID.randomUUID();
            Map<String, String> signedParams = new LinkedHashMap<>();
            signedParams.put("asset_folder", targetFolder);
            signedParams.put("folder", targetFolder);
            signedParams.put("overwrite", "true");
            signedParams.put("public_id", publicId);
            signedParams.put("timestamp", String.valueOf(timestamp));
            String signature = createSignature(signedParams);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new NamedByteArrayResource(uploadContent.bytes(), uploadContent.filename()))
                    .contentType(uploadContent.mediaType());
            builder.part("api_key", apiKey);
            signedParams.forEach(builder::part);
            builder.part("signature", signature);

            MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = builder.build();
            Duration uploadTimeout = resolveUploadTimeout();
            Map<?, ?> response = buildCloudinaryWebClient(uploadTimeout)
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
                                        log.warn("Cloudinary diary upload failed with status {} and body {}", cloudinaryResponse.statusCode(), errorBody);
                                        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary did not accept diary image");
                                    })
                    )
                    .bodyToMono(Map.class)
                    .timeout(uploadTimeout)
                    .retryWhen(cloudinaryUploadRetry())
                    .block();

            Object secureUrl = response != null ? response.get("secure_url") : null;
            if (secureUrl instanceof String url && !url.isBlank()) {
                log.info("Diary image uploaded to Cloudinary folder {}", targetFolder);
                return url;
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read image URL from Cloudinary");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Diary image upload failed", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload diary image", exception);
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
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
                    .doOnNext(response -> log.info("Deleted diary image {} from Cloudinary with result {}", publicId, response.get("result")))
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception exception) {
            log.warn("Could not delete diary image from Cloudinary: {}", publicId, exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose an image");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image cannot exceed 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must be an image");
        }
    }

    private void validateCloudinaryConfig() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary is not configured for diary images");
        }
    }

    private String resolveDiaryFolder() {
        return diaryFolder == null || diaryFolder.isBlank() ? "Diary" : diaryFolder.trim();
    }

    private Duration resolveUploadTimeout() {
        return Duration.ofSeconds(Math.max(10L, diaryUploadTimeoutSeconds));
    }

    private WebClient buildCloudinaryWebClient(Duration timeout) {
        HttpClient httpClient = HttpClient.create()
                .keepAlive(false)
                .responseTimeout(timeout);
        return webClientBuilder.clone()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private Retry cloudinaryUploadRetry() {
        return Retry.backoff(2, Duration.ofMillis(600))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(this::isTransientCloudinaryUploadError)
                .doBeforeRetry(signal -> log.warn(
                        "Retrying Cloudinary diary upload after transient network error: {}",
                        signal.failure().toString()
                ));
    }

    private boolean isTransientCloudinaryUploadError(Throwable throwable) {
        return hasCause(throwable, WebClientRequestException.class)
                || hasCause(throwable, SocketException.class)
                || hasCause(throwable, TimeoutException.class);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private UploadImageContent prepareUploadContent(MultipartFile file) throws Exception {
        byte[] originalBytes = file.getBytes();
        MediaType originalMediaType = resolveContentType(file);
        String originalFilename = sanitizeFileName(file.getOriginalFilename());

        if (!shouldCompress(file, originalMediaType)) {
            return new UploadImageContent(originalBytes, originalFilename, originalMediaType);
        }

        try {
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (sourceImage == null) {
                return new UploadImageContent(originalBytes, originalFilename, originalMediaType);
            }

            BufferedImage optimizedImage = resizeAndFlatten(sourceImage);
            byte[] optimizedBytes = encodeJpeg(optimizedImage);
            if (optimizedBytes.length == 0 || optimizedBytes.length >= originalBytes.length) {
                return new UploadImageContent(originalBytes, originalFilename, originalMediaType);
            }

            log.info(
                    "Optimized diary image before Cloudinary upload: {}KB -> {}KB",
                    originalBytes.length / 1024,
                    optimizedBytes.length / 1024
            );
            return new UploadImageContent(optimizedBytes, toJpegFileName(originalFilename), MediaType.IMAGE_JPEG);
        } catch (Exception exception) {
            log.debug("Could not optimize diary image before upload; using original file", exception);
            return new UploadImageContent(originalBytes, originalFilename, originalMediaType);
        }
    }

    private boolean shouldCompress(MultipartFile file, MediaType mediaType) {
        String subtype = mediaType.getSubtype().toLowerCase();
        return file.getSize() >= Math.max(0L, diaryCompressionMinBytes)
                && !subtype.contains("gif")
                && !subtype.contains("svg");
    }

    private BufferedImage resizeAndFlatten(BufferedImage sourceImage) {
        int maxWidth = Math.max(320, diaryImageMaxWidth);
        int maxHeight = Math.max(320, diaryImageMaxHeight);
        double scale = Math.min(1.0, Math.min((double) maxWidth / sourceImage.getWidth(), (double) maxHeight / sourceImage.getHeight()));
        int targetWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * scale));

        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return targetImage;
    }

    private byte[] encodeJpeg(BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            return new byte[0];
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(outputStream)) {
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(resolveImageQuality());
            }
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(image, null, null), writeParam);
            imageOutputStream.flush();
            return outputStream.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private float resolveImageQuality() {
        return Math.max(0.45f, Math.min(0.95f, diaryImageQuality));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not sign Cloudinary request", exception);
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
            return "diary.jpg";
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String toJpegFileName(String filename) {
        int extensionIndex = filename.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;
        return (baseName.isBlank() ? "diary" : baseName) + ".jpg";
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

        while (publicIdStartIndex < pathParts.length
                && (pathParts[publicIdStartIndex].matches("v\\d+")
                || pathParts[publicIdStartIndex].contains(",")
                || pathParts[publicIdStartIndex].matches("(?i)^(f|q|c|w|h|g|e|dpr|ar|r|b|a|o|x|y|z)_.*"))) {
            publicIdStartIndex++;
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

    private record UploadImageContent(byte[] bytes, String filename, MediaType mediaType) {
    }
}
