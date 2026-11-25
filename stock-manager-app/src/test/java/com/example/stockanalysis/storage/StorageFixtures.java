package com.example.stockanalysis.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

final class StorageFixtures {
    static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule()).disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    static StorageProperties settings(String path,boolean archive,boolean alerts){return new StorageProperties(true,archive,path,90,100,true,alerts,1000,80,90,14,false,5);}
    static DriverManagerDataSource database()throws Exception{
        var ds=new DriverManagerDataSource("jdbc:h2:mem:snapshot_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
        for(String file:List.of("08_screener.sql","11_unified_analysis.sql","14_daily_market_report.sql","18_snapshot_storage.sql"))new ResourceDatabasePopulator(new ByteArrayResource(Files.readAllBytes(Path.of("..","database","oracle","ddl",file)))).execute(ds);
        return ds;
    }
    static String history(int start,String observed){
        var out=new StringBuilder("{\n\"종목\":\"하이트진로\\n\",\"input\":{\"dailyCandles\":[");
        for(int i=start;i<start+120;i++){if(i>start)out.append(",\n ");var day=LocalDate.of(2025,1,1).plusDays(i);out.append("{\"businessDate\":\"").append(day).append("\",\"close\":").append(10000+i).append(",\"precise\":1.2300e+4}");}
        out.append("]},\"sources\":[{\"apiId\":\"ka10081\",\"records\":[");
        for(int i=start;i<start+120;i++){if(i>start)out.append(",\n ");var day=LocalDate.of(2025,1,1).plusDays(i);out.append("{\"stockCode\":\"000080\",\"businessDate\":\"").append(day).append("\",\"observedAt\":\"").append(observed).append("\",\"values\":{\"close\":\"").append(10000+i).append("\",\"volume\":\"123456\"}}");}
        return out.append("]}]}\n").toString();
    }
}
