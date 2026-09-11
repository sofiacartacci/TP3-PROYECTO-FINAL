
package com.systech.ms.list.ingestor.model;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.modelmapper.internal.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;

import com.systech.ms.list.model.searcher.Filters;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@Document(collection = "providers")
@Component
public class Provider {
    @Id
    @NonNull
    public String providerId;
    @NonNull
    public String providerName;
    @NonNull
    public Date lastUpdate;
    @NonNull
    public Integer processed;
    private Integer errorCount = 0;
    @NonNull
    private Map<String, Integer> errors;
    @NonNull
    public Date sysUpdate = null;

    private Boolean aborted = false;

    public ListInformation listInformation = null;

    @PersistenceConstructor
    public Provider(String providerId, String providerName, Date lastUpdate, Integer processed, Integer errorCount,
            Map<String, Integer> errors, Date sysUpdate, Boolean aborted) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.processed = processed;
        this.lastUpdate = lastUpdate;
        this.errorCount = Objects.firstNonNull(errorCount, 0);
        this.errors = errors;
        this.sysUpdate = sysUpdate;
        this.aborted = Objects.firstNonNull(aborted, false);
    }

    public Provider(String providerId, String providerName) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.lastUpdate = Date.from(Instant.EPOCH);
        this.processed = 0;
        this.errorCount = Objects.firstNonNull(errorCount, 0);
        this.errors = new HashMap<String, Integer>();
        this.aborted = Objects.firstNonNull(aborted, false);
        this.listInformation = new ListInformation(new Filters());

    }
}