package com.phaithanhcong.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallSignalDTO {
    @NotNull(message = "Signal type cannot be null")
    private String type; 

    private Long fromUserId;

    @NotNull(message = "toUserId cannot be null")
    private Long toUserId;

    private String callType; 
    
    private Long callLogId;
    
    private String payload; 
    
    private Boolean stateChanged; 
}
