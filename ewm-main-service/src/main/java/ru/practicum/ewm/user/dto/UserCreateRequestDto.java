package ru.practicum.ewm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserCreateRequestDto {
    @NotBlank
    @Size(min = 2, max = 255)
    private String name;
    @NotBlank
    @Email
    @Size(min = 6, max = 255)
    private String email;
}
