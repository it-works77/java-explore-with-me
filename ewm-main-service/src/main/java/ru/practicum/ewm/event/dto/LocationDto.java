package ru.practicum.ewm.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LocationDto(@Valid @NotNull @Min(-90) @Max(90) Double lat,
                          @Valid @NotNull @Min(-180) @Max(180) Double lon) {
}
