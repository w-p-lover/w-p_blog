package com.ican.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRegistrationMessage {
    private String requestId;
    private String dispatchToken;
}
