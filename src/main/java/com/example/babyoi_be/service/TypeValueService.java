package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.TypeValueResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TypeValueService {
    Map<String, List<TypeValueResponse>> getValuesByCodes(List<String> typeCodes);
    boolean existsValueCode(String typeCode, String valueCode);
    boolean existsValueText(String typeCode, String valueText);
    boolean existsValueNumber(String typeCode, BigDecimal valueNumber);
}
