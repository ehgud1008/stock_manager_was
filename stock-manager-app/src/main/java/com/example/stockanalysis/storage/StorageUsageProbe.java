package com.example.stockanalysis.storage;

import java.util.List;

public interface StorageUsageProbe {
    record Consumer(String object,String type,long bytes){}
    record Usage(long allocatedBytes,Long capacityBytes,String scope,List<Consumer> consumers){}
    Usage measure();
}
