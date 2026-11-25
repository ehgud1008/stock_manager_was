package com.example.stockanalysis.unified.report;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class ReportDeliveryService {
    private final ReportStore store;
    private final TelegramProperties settings;
    private final TelegramGateway gateway;
    private final Clock clock;
    private final AtomicBoolean active=new AtomicBoolean();
    public ReportDeliveryService(ReportStore store,TelegramProperties settings,TelegramGateway gateway,Clock clock){this.store=store;this.settings=settings;this.gateway=gateway;this.clock=clock;}
    public void tick(){
        if(!settings.enabled()||!active.compareAndSet(false,true))return;
        try {
            store.claimDelivery(clock.millis(),settings.chatId()).ifPresent(delivery->{
                TelegramGateway.Result result;
                try{result=gateway.send(delivery.destination(),delivery.text());}
                catch(RuntimeException e){result=new TelegramGateway.Result("UNKNOWN",null,"DELIVERY_UNCONFIRMED",0);}
                String state=result.state();
                long wait=Math.max(1,Math.min(result.retryAfterSeconds(),86400));
                long next=clock.millis()+wait*1000;
                if(state.equals("RETRY_WAIT") && (delivery.attempts()>=3||next>=delivery.deadline()))state="FAILED";
                store.delivered(delivery,state,result.messageId(),result.errorCode(),next);
            });
        } finally {active.set(false);}
    }
}
