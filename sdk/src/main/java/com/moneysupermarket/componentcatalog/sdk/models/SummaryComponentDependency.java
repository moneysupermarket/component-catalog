package com.moneysupermarket.componentcatalog.sdk.models;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

import static com.moneysupermarket.componentcatalog.sdk.utils.ListUtils.createUnmodifiableList;

@Value
@With
@Builder(toBuilder = true)
@Jacksonized
public class SummaryComponentDependency implements ObjectWithSourceIndexAndTargetIndex {

    @Nullable
    @Min(0)
    Integer sourceIndex;
    @NotNull
    @Min(0)
    Integer targetIndex;
    @NotNull
    List<@NotNull @Min(0) Integer> relatedIndexes;
    @NotNull
    Boolean manual;
    Integer sampleSize;
    LocalDateTime startTimestamp;
    LocalDateTime endTimestamp;
    SummaryComponentDependencyDuration duration;

    public SummaryComponentDependency(Integer sourceIndex, Integer targetIndex, List<Integer> relatedIndexes, Boolean manual, Integer sampleSize,
            LocalDateTime startTimestamp, LocalDateTime endTimestamp, SummaryComponentDependencyDuration duration) {
        this.sourceIndex = sourceIndex;
        this.targetIndex = targetIndex;
        this.relatedIndexes = createUnmodifiableList(relatedIndexes);
        this.manual = manual;
        this.sampleSize = sampleSize;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.duration = duration;
    }
}
