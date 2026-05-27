package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.domain.entity.VaccineType;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.VaccineRecordRepository;
import com.example.babyoi_be.repository.VaccineTypeRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.VaccineRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaccineRecordServiceImpl implements VaccineRecordService {

    private static final List<Long> DUPLICATE_CHECK_STATUSES = List.of(
            Constants.TABLE_STATUS.SUCCESS,
            Constants.TABLE_STATUS.PENDING
    );

    private final VaccineRecordRepository vaccineRecordRepository;
    private final VaccineTypeRepository vaccineTypeRepository;
    private final ProfileRepository profileRepository;

    private void validateProfileOwnership(Long profileId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            Long currentUserId = userDetails.getId();
            Profile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new RuntimeException("Profile not found"));
            if (!profile.getUser().getId().equals(currentUserId)) {
                throw new RuntimeException("You do not have permission to access this profile's records");
            }
        }
    }

    private void validateUniqueVaccineType(Long profileId, Long vaccineTypeId, Long currentRecordId) {
        boolean exists = currentRecordId == null
                ? vaccineRecordRepository.existsByProfileIdAndVaccineTypeIdAndStatusIn(profileId, vaccineTypeId, DUPLICATE_CHECK_STATUSES)
                : vaccineRecordRepository.existsByProfileIdAndVaccineTypeIdAndStatusInAndIdNot(profileId, vaccineTypeId, DUPLICATE_CHECK_STATUSES, currentRecordId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loại vaccin này đã tồn tại cho hồ sơ này");
        }
    }

    @Override
    @Transactional
    public VaccineRecordResponse createVaccineRecord(VaccineRecordRequest request) {
        validateProfileOwnership(request.getProfileId());
        validateUniqueVaccineType(request.getProfileId(), request.getVaccineTypeId(), null);

        VaccineType vaccineType = vaccineTypeRepository.findById(request.getVaccineTypeId())
                .orElseThrow(() -> new RuntimeException("Vaccine type not found"));

        VaccineRecord record = VaccineRecord.builder()
                .profileId(request.getProfileId())
                .vaccineType(vaccineType)
                .injectionDate(request.getInjectionDate())
                .location(request.getLocation())
                .price(request.getPrice())
                .note(request.getNote())
                .status(request.getStatus() != null ? request.getStatus() : Constants.TABLE_STATUS.PENDING)
                .createdAt(LocalDate.now())
                .build();

        return mapToResponse(vaccineRecordRepository.save(record));
    }

    @Override
    @Transactional
    public VaccineRecordResponse updateVaccineRecord(Long id, VaccineRecordRequest request) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaccine record not found"));

        validateProfileOwnership(record.getProfileId());
        // Also validate if the new profileId in request (if different) is owned by the user
        if (!record.getProfileId().equals(request.getProfileId())) {
            validateProfileOwnership(request.getProfileId());
        }

        validateUniqueVaccineType(request.getProfileId(), request.getVaccineTypeId(), id);

        VaccineType vaccineType = vaccineTypeRepository.findById(request.getVaccineTypeId())
                .orElseThrow(() -> new RuntimeException("Vaccine type not found"));

        record.setProfileId(request.getProfileId());
        record.setVaccineType(vaccineType);
        record.setInjectionDate(request.getInjectionDate());
        record.setLocation(request.getLocation());
        record.setPrice(request.getPrice());
        record.setNote(request.getNote());
        record.setStatus(request.getStatus());
        record.setUpdatedAt(LocalDate.now());

        return mapToResponse(vaccineRecordRepository.save(record));
    }

    @Override
    @Transactional
    public void deleteVaccineRecord(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaccine record not found"));
        
        validateProfileOwnership(record.getProfileId());

        // Soft delete
        record.setStatus(Constants.TABLE_STATUS.DELETED);
        record.setUpdatedAt(LocalDate.now());
        vaccineRecordRepository.save(record);
    }

    @Override
    public VaccineRecordResponse getVaccineRecordById(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaccine record not found"));
        
        validateProfileOwnership(record.getProfileId());
        
        return mapToResponse(record);
    }

    @Override
    public List<VaccineRecordResponse> getVaccineRecordsByStatus(Long profileId, Long status) {
        return getVaccineRecordsByStatus(profileId, status, null);
    }

    @Override
    public List<VaccineRecordResponse> getVaccineRecordsByStatus(Long profileId, Long status, Integer limit) {
        validateProfileOwnership(profileId);

        if (limit == null || limit <= 0) {
            return vaccineRecordRepository.findByProfileIdAndStatus(profileId, status, getSortForStatus(status))
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return vaccineRecordRepository.findByProfileIdAndStatus(profileId, status, PageRequest.of(0, limit, getSortForStatus(status)))
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<VaccineRecordResponse> getVaccineRecordsPage(Long profileId, Long status, Integer page, Integer size) {
        validateProfileOwnership(profileId);

        int pageIndex = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 50) : 5;

        Page<VaccineRecord> vaccineRecordPage = vaccineRecordRepository.findByProfileIdAndStatus(
                profileId,
                status,
                PageRequest.of(pageIndex, pageSize, getSortForStatus(status))
        );

        return PageResponse.<VaccineRecordResponse>builder()
                .content(vaccineRecordPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(vaccineRecordPage.getNumber())
                .size(vaccineRecordPage.getSize())
                .totalElements(vaccineRecordPage.getTotalElements())
                .totalPages(vaccineRecordPage.getTotalPages())
                .first(vaccineRecordPage.isFirst())
                .last(vaccineRecordPage.isLast())
                .build();
    }

    @Override
    public long countVaccineRecordsByStatus(Long profileId, Long status) {
        validateProfileOwnership(profileId);
        return vaccineRecordRepository.countByProfileIdAndStatus(profileId, status);
    }

    @Override
    public Map<String, Long> getVaccineStatistics(Long profileId) {
        validateProfileOwnership(profileId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.PENDING));
        stats.put("canceled", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.CANCELED));
        stats.put("success", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.SUCCESS));
        return stats;
    }

    private VaccineRecordResponse mapToResponse(VaccineRecord record) {
        VaccineType type = record.getVaccineType();
        return VaccineRecordResponse.builder()
                .id(record.getId())
                .profileId(record.getProfileId())
                .vaccineTypeId(type != null ? type.getId() : null)
                .vaccineTypeName(type != null ? type.getName() : null)
                .vaccineTypeDescription(type != null ? type.getDescription() : null)
                .injectionDate(record.getInjectionDate())
                .location(record.getLocation())
                .price(record.getPrice())
                .note(record.getNote())
                .status(record.getStatus())
                .build();
    }

    private Sort getSortForStatus(Long status) {
        return Constants.TABLE_STATUS.SUCCESS.equals(status)
                ? Sort.by(Sort.Direction.DESC, "injectionDate").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.ASC, "injectionDate").and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
