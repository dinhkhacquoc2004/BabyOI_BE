package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.domain.dto.respone.VaccineTypeResponse;
import com.example.babyoi_be.domain.entity.VaccineType;
import com.example.babyoi_be.repository.VaccineTypeRepository;
import com.example.babyoi_be.service.VaccineTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccineTypeServiceImpl implements VaccineTypeService {

    private final VaccineTypeRepository vaccineTypeRepository;

    @Override
    public List<VaccineTypeResponse> getVaccineTypes(String keyword) {
        List<VaccineType> allTypes = vaccineTypeRepository.findByStatus(1L);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return allTypes.stream().map(this::mapToResponse).collect(Collectors.toList());
        }

        String normalizedKeyword = removeAccent(keyword.toLowerCase());

        return allTypes.stream()
                .filter(type -> {
                    String nameLower = type.getName().toLowerCase();
                    String nameUnaccented = removeAccent(nameLower);
                    return nameLower.contains(keyword.toLowerCase()) || nameUnaccented.contains(normalizedKeyword);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
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
