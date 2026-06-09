package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {
    private Long id;
    private String name;
    private String region;
    private String address;
    private Long status;
}
