package com.example.babyoi_be.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "{auth.username.required}")
    private String userName;

    @Email(message = "{auth.email.invalid}")
    @NotBlank(message = "{auth.email.required}")
    private String email;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, message = "{auth.password.min-length}")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$",
            message = "{auth.password.complexity}"
    )
    private String password;
}
