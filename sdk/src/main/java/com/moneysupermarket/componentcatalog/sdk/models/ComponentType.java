package com.moneysupermarket.componentcatalog.sdk.models;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Value
@With
@Builder(toBuilder = true)
@Jacksonized
public class ComponentType implements ObjectWithId, ObjectWithReference {

    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9]*(-[a-z0-9]+)*")
    String id;
    @NotBlank
    String description;
    String notes;

    @Override
    public String reference() {
        return id;
    }
}
