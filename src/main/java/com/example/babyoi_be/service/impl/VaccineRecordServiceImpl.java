package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.domain.entity.VaccineType;
import com.example.babyoi_be.repository.VaccineRecordRepository;
import com.example.babyoi_be.repository.VaccineTypeRepository;
import com.example.babyoi_be.service.VaccineRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccineRecordServiceImpl implements VaccineRecordService {

    private final VaccineRecordRepository vaccineRecordRepository;
    private final VaccineTypeRepository vaccineTypeRepository;

    private static final List<Long> VALID_STATUSES = Arrays.asList(
            Constants.TABLE_STATUS.PENDING,
            Constants.TABLE_STATUS.SUCCESS,
            Constants.TABLE_STATUS.CANCELED
    );

    @Override
    @Transactional
    public VaccineRecordResponse createVaccineRecord(VaccineRecordRequest request) {
        VaccineType vaccineType = vaccineTypeRepository.findById(request.getVaccineTypeId())
                .orElseThrow(() -> new RuntimeException("Vaccine type not found"));

        VaccineRecord record = VaccineRecord.builder()
                .profileId(request.getProfileId())
                .vaccineType(vaccineType)
                .injectionDate(request.getInjectionDate())
                .location(request.getLocation())
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

        VaccineType vaccineType = vaccineTypeRepository.findById(request.getVaccineTypeId())
                .orElseThrow(() -> new RuntimeException("Vaccine type not found"));

        record.setVaccineType(vaccineType);
        record.setInjectionDate(request.getInjectionDate());
        record.setLocation(request.getLocation());
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
        
        // Soft delete
        record.setStatus(Constants.TABLE_STATUS.DELETED);
        record.setUpdatedAt(LocalDate.now());
        vaccineRecordRepository.save(record);
    }

    @Override
    public VaccineRecordResponse getVaccineRecordById(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaccine record not found"));
        return mapToResponse(record);
    }

    @Override
    public List<VaccineRecordResponse> getVaccineRecordsByStatus(Long profileId, Long status) {
        return vaccineRecordRepository.findByProfileIdAndStatus(profileId, status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countVaccineRecordsByStatus(Long profileId, Long status) {
        return vaccineRecordRepository.countByProfileIdAndStatus(profileId, status);
    }

    @Override
    public Map<String, Long> getVaccineStatistics(Long profileId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.PENDING));
        stats.put("canceled", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.CANCELED));
        stats.put("success", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.SUCCESS));
        return stats;
    }

    private VaccineRecordResponse mapToResponse(VaccineRecord record) {
        return VaccineRecordResponse.builder()
                .id(record.getId())
                .profileId(record.getProfileId())
                .vaccineTypeId(record.getVaccineType() != null ? record.getVaccineType().getId() : null)
                .vaccineTypeName(record.getVaccineType() != null ? record.getVaccineType().getName() : null)
                .injectionDate(record.getInjectionDate())
                .location(record.getLocation())
                .note(record.getNote())
                .status(record.getStatus())
                .build();
    }
}
