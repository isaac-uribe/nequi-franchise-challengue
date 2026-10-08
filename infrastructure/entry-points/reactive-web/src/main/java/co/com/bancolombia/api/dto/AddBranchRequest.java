package co.com.bancolombia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AddBranchRequest(@Schema(maxLength = 100) String name) {
}
