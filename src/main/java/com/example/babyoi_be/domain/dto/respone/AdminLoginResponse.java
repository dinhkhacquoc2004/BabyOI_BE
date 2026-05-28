package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {
    private Long id;
    private String email;
    private String userName;
    private String role;
}
