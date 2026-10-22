package com.pharmacy.system.dto;

import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DrugResponseDTO {

    private String drugCode;
    private String brandName;
    private String genericName;
    private String category;
    private DosageForm dosageForm;
    private ScheduleType scheduleType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
