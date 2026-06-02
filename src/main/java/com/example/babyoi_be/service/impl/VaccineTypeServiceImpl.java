package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.utils.TextSearchUtils;
import com.example.babyoi_be.domain.dto.respone.VaccineTypeResponse;
import com.example.babyoi_be.domain.entity.VaccineType;
import com.example.babyoi_be.repository.VaccineTypeRepository;
import com.example.babyoi_be.service.VaccineTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccineTypeServiceImpl implements VaccineTypeService {

    private final VaccineTypeRepository vaccineTypeRepository;

    @Override
    public List<VaccineTypeResponse> getVaccineTypes(String keyword) {
        List<VaccineType> allTypes = vaccineTypeRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE);

        if (keyword == null || keyword.trim().isEmpty()) {
            return allTypes.stream().map(this::mapToResponse).collect(Collectors.toList());
        }

        return allTypes.stream()
                .filter(type -> TextSearchUtils.contains(type.getName(), keyword))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VaccineTypeResponse mapToResponse(VaccineType type) {
        return VaccineTypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .description(type.getDescription())
                .requiredAge(type.getRequiredAge())
                .build();
    }
}
