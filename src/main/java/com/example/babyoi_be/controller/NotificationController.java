package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.DeviceTokenRequest;
import com.example.babyoi_be.domain.dto.request.NotificationSettingRequest;
import com.example.babyoi_be.domain.dto.respone.NotificationResponse;
import com.example.babyoi_be.domain.dto.respone.NotificationSettingResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> getNotifications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "false") boolean archived) {
        return notificationService.getNotifications(page, size, archived);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("count", notificationService.getUnreadCount());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        notificationService.archive(id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @PostMapping("/device-tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(@Valid @RequestBody DeviceTokenRequest request) {
        notificationService.registerDeviceToken(request);
    }

    @DeleteMapping("/device-tokens/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDeviceToken(@PathVariable String deviceId) {
        notificationService.deactivateDeviceToken(deviceId);
    }

    @GetMapping("/settings")
    public NotificationSettingResponse getSettings() {
        return notificationService.getSettings();
    }

    @PatchMapping("/settings")
    public NotificationSettingResponse updateSettings(@RequestBody NotificationSettingRequest request) {
        return notificationService.updateSettings(request);
    }
}
