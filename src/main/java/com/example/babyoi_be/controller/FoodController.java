package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.IngredientFoodSuggestionRequest;
import com.example.babyoi_be.domain.dto.respone.FoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.FoodResponse;
import com.example.babyoi_be.domain.dto.respone.FavoriteFoodResponse;
import com.example.babyoi_be.domain.dto.respone.IngredientFoodSuggestionResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.dto.respone.TypeValueResponse;
import com.example.babyoi_be.service.FoodService;
import com.example.babyoi_be.service.TypeValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final TypeValueService typeValueService;

    @GetMapping
    public List<FoodResponse> getFoods() {
        return foodService.getFoods();
    }

    @GetMapping("/filter-options")
    public Map<String, List<TypeValueResponse>> getFilterOptions() {
        return typeValueService.getValuesByCodes(List.of("FOOD_FUNCTION_CODE", "FOOD_ADVICE_FOR"));
    }

    @GetMapping("/page")
    public PageResponse<FoodResponse> getFoodsPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long functionCode,
            @RequestParam(required = false) String advanceFor,
            @RequestParam(required = false) String search) {
        return foodService.getFoodsPage(page, size, functionCode, advanceFor, search);
    }

    @GetMapping("/{foodId}")
    public FoodResponse getFoodDetail(@PathVariable Long foodId) {
        return foodService.getFoodDetail(foodId);
    }

    @GetMapping("/detail/{foodId}")
    public FoodResponse getFoodDetailById(@PathVariable Long foodId) {
        return foodService.getFoodDetail(foodId);
    }

    @GetMapping("/favorites/profile/{profileId}")
    public List<FoodResponse> getFavoriteFoodsByProfileId(@PathVariable Long profileId) {
        return foodService.getFavoriteFoodsByProfileId(profileId);
    }

    @GetMapping("/favorites/profile/{profileId}/ids")
    public List<Long> getFavoriteFoodIdsByProfileId(@PathVariable Long profileId) {
        return foodService.getFavoriteFoodIdsByProfileId(profileId);
    }

    @PostMapping("/favorites/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteFoodResponse addFavoriteFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        return foodService.addFavoriteFood(profileId, foodId);
    }

    @DeleteMapping("/favorites/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavoriteFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        foodService.deleteFavoriteFood(profileId, foodId);
    }

    @GetMapping("/restricted/profile/{profileId}")
    public List<FoodResponse> getRestrictedFoodsByProfileId(@PathVariable Long profileId) {
        return foodService.getRestrictedFoodsByProfileId(profileId);
    }

    @GetMapping("/restricted/profile/{profileId}/ids")
    public List<Long> getRestrictedFoodIdsByProfileId(@PathVariable Long profileId) {
        return foodService.getRestrictedFoodIdsByProfileId(profileId);
    }

    @PostMapping("/restricted/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteFoodResponse addRestrictedFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        return foodService.addRestrictedFood(profileId, foodId);
    }

    @DeleteMapping("/restricted/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRestrictedFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        foodService.deleteRestrictedFood(profileId, foodId);
    }

    @GetMapping("/{foodId}/ingredients")
    public List<FoodIngredientResponse> getIngredientsByFoodId(@PathVariable Long foodId) {
        return foodService.getIngredientsByFoodId(foodId);
    }

    @PostMapping(value = "/suggestions/from-ingredients", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngredientFoodSuggestionResponse suggestFoodsFromIngredients(
            @RequestParam(required = false) Long profileId,
            @RequestParam(required = false) List<String> ingredientNames,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) Integer limit) {
        return foodService.suggestFoodsFromIngredients(profileId, ingredientNames, image, limit);
    }

    @PostMapping(value = "/suggestions/from-ingredients-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public IngredientFoodSuggestionResponse suggestFoodsFromIngredientsJson(
            @RequestBody IngredientFoodSuggestionRequest request) {
        MultipartFile image = buildImageFile(request);
        return foodService.suggestFoodsFromIngredients(
                request.getProfileId(),
                request.getIngredientNames(),
                image,
                request.getLimit());
    }

    private MultipartFile buildImageFile(IngredientFoodSuggestionRequest request) {
        if (request == null || request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            return null;
        }

        String imageBase64 = request.getImageBase64().trim();
        String contentType = request.getImageMimeType();
        int commaIndex = imageBase64.indexOf(',');
        if (imageBase64.startsWith("data:") && commaIndex > 0) {
            String metadata = imageBase64.substring(5, commaIndex);
            int semicolonIndex = metadata.indexOf(';');
            if ((contentType == null || contentType.isBlank()) && semicolonIndex > 0) {
                contentType = metadata.substring(0, semicolonIndex);
            }
            imageBase64 = imageBase64.substring(commaIndex + 1);
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            return new ByteArrayMultipartFile(
                    "image",
                    request.getImageFilename() != null && !request.getImageFilename().isBlank()
                            ? request.getImageFilename()
                            : "ingredients.jpg",
                    contentType != null && !contentType.isBlank() ? contentType : MediaType.IMAGE_JPEG_VALUE,
                    bytes);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu ảnh base64 không hợp lệ", exception);
        }
    }

    private record ByteArrayMultipartFile(
            String name,
            String originalFilename,
            String contentType,
            byte[] bytes) implements MultipartFile {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes != null ? bytes.length : 0;
        }

        @Override
        public byte[] getBytes() {
            return bytes != null ? bytes : new byte[0];
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(getBytes());
        }

        @Override
        public void transferTo(File dest) throws IOException {
            org.springframework.util.FileCopyUtils.copy(getBytes(), dest);
        }
    }
}
