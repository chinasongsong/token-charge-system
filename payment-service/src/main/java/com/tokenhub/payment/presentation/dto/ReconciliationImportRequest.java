package com.tokenhub.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReconciliationImportRequest(
    @NotBlank String channel,
    @NotNull LocalDate billDate,
    @NotBlank String sourceName,
    @NotBlank String csv
) {}
