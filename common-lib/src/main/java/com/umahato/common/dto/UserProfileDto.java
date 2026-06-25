package com.umahato.common.dto;

public record UserProfileDto(
        Long id,
        String email,
        String name,
        String preferences
) implements java.io.Serializable {
}
