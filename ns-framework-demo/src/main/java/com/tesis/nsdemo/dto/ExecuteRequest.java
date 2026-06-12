package com.tesis.nsdemo.dto;

import jakarta.validation.constraints.NotBlank;

public record ExecuteRequest(@NotBlank String input) {
}
