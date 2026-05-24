package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.respone.VaccineTypeResponse;
import com.example.babyoi_be.service.VaccineTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vaccine-types")
@RequiredArgsConstructor
public class VaccineTypeController {

    private final VaccineTypeService vaccineTypeService;

    @GetMapping
    public List<VaccineTypeResponse> getAll(@RequestParam(required = false) String keyword) {
        return vaccineTypeService.getVaccineTypes(keyword);
    }
}
