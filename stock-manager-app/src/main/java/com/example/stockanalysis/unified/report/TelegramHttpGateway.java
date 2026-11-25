package com.example.stockanalysis.unified.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TelegramHttpGateway implements TelegramGateway {
    private final TelegramProperties settings;
    private final ObjectMapper mapper;
    private final HttpClient client;
    public TelegramHttpGateway(TelegramProperties settings,ObjectMapper mapper){
        this.settings=settings;this.mapper=mapper;this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    public Result send(String destination,String text){
        if(!settings.enabled()||!settings.chatId().equals(destination))return new Result("FAILED",null,"DESTINATION_NOT_CONFIGURED",0);
        try {
            var request=HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot"+settings.botToken()+"/sendMessage"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("chat_id",destination,"text",text,"link_preview_options",Map.of("is_disabled",true))))).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            return interpret(response.statusCode(),response.body());
        }catch(java.net.ConnectException e){return new Result("RETRY_WAIT",null,"CONNECTION_FAILED",60);}
        catch(InterruptedException e){Thread.currentThread().interrupt();return new Result("UNKNOWN",null,"REQUEST_INTERRUPTED",0);}
        catch(Exception e){return new Result("UNKNOWN",null,"DELIVERY_UNCONFIRMED",0);}
    }
    public Result interpret(int status,String body){
        try {
            var response=mapper.readTree(body);
            if(status==200 && response.path("ok").asBoolean() && response.path("result").path("message_id").canConvertToLong())
                return new Result("SENT",response.path("result").path("message_id").asText(),null,0);
            if(response.has("ok")&&!response.path("ok").asBoolean()) {
                int code=response.path("error_code").asInt(status);
                if(code==429)return new Result("RETRY_WAIT",null,"RATE_LIMIT",Math.max(1,response.path("parameters").path("retry_after").asLong(60)));
                if(code>=500)return new Result("RETRY_WAIT",null,"REMOTE_REJECTED",60);
                return new Result("FAILED",null,"REMOTE_REJECTED_"+code,0);
            }
        }catch(Exception ignored){ /* The URI, response body and exception may contain credentials; never log them. */ }
        return new Result("UNKNOWN",null,"DELIVERY_UNCONFIRMED",0);
    }
}
