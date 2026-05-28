package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.respone.TypeValueResponse;
import com.example.babyoi_be.domain.entity.TypeValue;
import com.example.babyoi_be.repository.TypeValueRepository;
import com.example.babyoi_be.service.TypeValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TypeValueServiceImpl implements TypeValueService {

    private final TypeValueRepository typeValueRepository;

    @Override
    public Map<String, List<TypeValueResponse>> getValuesByCodes(List<String> typeCodes) {
        List<String> normalizedCodes = typeCodes.stream()
                .filter(typeCode -> typeCode != null && !typeCode.isBlank())
                .map(this::normalizeCode)
                .distinct()
                .toList();

        return typeValueRepository.findByTypeCodeCodeInAndStatusOrderByTypeCodeCodeAscSortOrderAscIdAsc(
                        normalizedCodes, Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.groupingBy(TypeValueResponse::getTypeCode));
    }

    @Override
    public boolean existsValueCode(String typeCode, String valueCode) {
        if (typeCode == null || typeCode.isBlank() || valueCode == null || valueCode.isBlank()) {
            return false;
        }
        return typeValueRepository.existsByTypeCodeCodeIgnoreCaseAndValueCodeIgnoreCaseAndStatus(
                normalizeCode(typeCode), valueCode.trim(), Constants.TABLE_STATUS.ACTIVE);
    }

    @Override
    public boolean existsValueText(String typeCode, String valueText) {
        if (typeCode == null || typeCode.isBlank() || valueText == null || valueText.isBlank()) {
            return false;
        }
        return typeValueRepository.existsByTypeCodeCodeIgnoreCaseAndValueTextIgnoreCaseAndStatus(
                normalizeCode(typeCode), valueText.trim(), Constants.TABLE_STATUS.ACTIVE);
    }

    @Override
    public boolean existsValueNumber(String typeCode, BigDecimal valueNumber) {
        if (typeCode == null || typeCode.isBlank() || valueNumber == null) {
            return false;
        }
        return typeValueRepository.existsByTypeCodeCodeIgnoreCaseAndValueNumberAndStatus(
                normalizeCode(typeCode), valueNumber, Constants.TABLE_STATUS.ACTIVE);
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private TypeValueResponse mapToResponse(TypeValue typeValue) {
        return TypeValueResponse.builder()
                .id(typeValue.getId())
                .typeCode(typeValue.getTypeCode() != null ? typeValue.getTypeCode().getCode() : null)
                .valueCode(typeValue.getValueCode())
                .valueName(typeValue.getValueName())
                .valueText(typeValue.getValueText())
                .valueNumber(typeValue.getValueNumber())
                .description(typeValue.getDescription())
                .sortOrder(typeValue.getSortOrder())
                .status(typeValue.getStatus())
                .build();
    }
}
