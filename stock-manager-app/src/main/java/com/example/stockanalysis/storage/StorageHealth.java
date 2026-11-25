package com.example.stockanalysis.storage;

/** Allocation trend, not a promise of free space or the exact date Oracle will reach its limit. */
public final class StorageHealth {
    private StorageHealth(){}
    public record Assessment(String level,Double allocatedPercent,Long dailyGrowthBytes,Long estimatedDays){}
    public static Assessment assess(StorageUsageProbe.Usage usage,Long previousBytes,long elapsedMillis,StorageProperties settings){
        Long growth=previousBytes==null||elapsedMillis<3600000?null:Math.max(0,Math.round((usage.allocatedBytes()-previousBytes)*86400000.0/elapsedMillis));
        Double percent=usage.capacityBytes()==null?null:100.0*usage.allocatedBytes()/usage.capacityBytes();
        Long days=growth==null||growth==0||usage.capacityBytes()==null?null:Math.max(0,(usage.capacityBytes()-usage.allocatedBytes())/growth);
        String level=percent==null?"UNKNOWN":percent>=settings.criticalPercent()?"CRITICAL":percent>=settings.warningPercent()||days!=null&&days<=settings.warningDays()?"WARNING":"NORMAL";
        return new Assessment(level,percent,growth,days);
    }
}
