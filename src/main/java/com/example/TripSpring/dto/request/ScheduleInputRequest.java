package com.example.TripSpring.dto.request;

import com.example.TripSpring.domain.InputType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ScheduleInputRequest {
    @NotBlank(message = "Input cannot be blank")
    private String input;
    
    @NotNull(message = "Input type must be specified")
    private InputType type;
}