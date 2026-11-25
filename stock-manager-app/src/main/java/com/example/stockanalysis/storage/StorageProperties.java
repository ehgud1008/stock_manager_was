package com.example.stockanalysis.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("storage.snapshots")
public record StorageProperties(@DefaultValue("true") boolean deduplicationEnabled,
        @DefaultValue("false") boolean archiveEnabled,@DefaultValue("") String archiveDirectory,
        @DefaultValue("90") int hotTradingDays,@DefaultValue("500") int archiveBatchSize,
        @DefaultValue("true") boolean monitoringEnabled,@DefaultValue("true") boolean alertsEnabled,
        @DefaultValue("0") long capacityBytes,@DefaultValue("80") int warningPercent,
        @DefaultValue("90") int criticalPercent,@DefaultValue("14") int warningDays,
        @DefaultValue("false") boolean legacyMigrationEnabled,@DefaultValue("25") int legacyBatchSize){
    public StorageProperties{
        if(hotTradingDays<20||hotTradingDays>1000||archiveBatchSize<1||archiveBatchSize>1000||capacityBytes<0||warningPercent<1||criticalPercent<=warningPercent||criticalPercent>100||warningDays<1||legacyBatchSize<1||legacyBatchSize>100)throw new IllegalArgumentException("Invalid storage policy");
        archiveDirectory=archiveDirectory==null?"":archiveDirectory.trim();
        if(archiveEnabled&&archiveDirectory.isEmpty())throw new IllegalArgumentException("Archive directory is required before automatic archiving can be enabled");
    }
}
