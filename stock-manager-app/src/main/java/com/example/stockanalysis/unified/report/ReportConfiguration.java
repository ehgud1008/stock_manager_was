package com.example.stockanalysis.unified.report;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
@EnableConfigurationProperties({ReportProperties.class,TelegramProperties.class})
public class ReportConfiguration {
    @Bean(destroyMethod="close") public ReportWorker reportWorker(DailyReportService reports,ReportDeliveryService deliveries){return new ReportWorker(reports,deliveries);}
    public static final class ReportWorker implements AutoCloseable {
        private final DailyReportService reports;private final ReportDeliveryService deliveries;
        private final ExecutorService pool=Executors.newFixedThreadPool(2,r->{var t=new Thread(r,"daily-market-report");t.setDaemon(true);return t;});
        private final AtomicBoolean reporting=new AtomicBoolean(),sending=new AtomicBoolean();
        private volatile boolean ready;
        ReportWorker(DailyReportService reports,ReportDeliveryService deliveries){this.reports=reports;this.deliveries=deliveries;}
        @EventListener(ApplicationReadyEvent.class) public void start(){ready=true;poll();}
        @Scheduled(fixedDelay=5000,initialDelay=5000) public void poll(){if(ready){enqueue(reporting,reports::tick);enqueue(sending,deliveries::tick);}}
        private void enqueue(AtomicBoolean active,Runnable task){
            if(!active.compareAndSet(false,true))return;
            try{pool.execute(()->{try{task.run();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Report worker deferred: type={}",e.getClass().getSimpleName());}finally{active.set(false);}});}
            catch(RejectedExecutionException e){active.set(false);}
        }
        public void close(){ready=false;pool.shutdownNow();}
    }
}
