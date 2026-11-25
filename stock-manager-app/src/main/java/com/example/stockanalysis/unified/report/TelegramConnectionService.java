package com.example.stockanalysis.unified.report;

import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Read-only Telegram checks. Never returns raw bodies, tokens, URLs or chat identities. */
@Service
public class TelegramConnectionService {
    public record Check(String state,String botUsername,String chatType,boolean automaticDeliveryEnabled,String message) {}
    private final TelegramProperties settings;
    private final ObjectMapper mapper;
    private final HttpClient client;
    @Autowired
    public TelegramConnectionService(TelegramProperties settings,ObjectMapper mapper){
        this(settings,mapper,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build());
    }
    TelegramConnectionService(TelegramProperties settings,ObjectMapper mapper,HttpClient client){this.settings=settings;this.mapper=mapper;this.client=client;}
    public Check check(){
        if(settings.botToken()==null||!settings.botToken().matches("[0-9]+:[A-Za-z0-9_-]+"))
            return result("TOKEN_NOT_CONFIGURED",null,null,"서버에 봇 토큰을 설정하세요.");
        if(settings.chatId()==null||!settings.chatId().matches("-?[0-9]+|@[A-Za-z0-9_]{5,}"))
            return result("CHAT_NOT_CONFIGURED",null,null,"서버에 알림을 받을 대화방 ID를 설정하세요.");
        try {
            var me=call("getMe",Map.of());
            if(!me.path("ok").asBoolean())return rejected(me,true);
            var bot=me.path("result");
            if(!bot.path("is_bot").asBoolean()||!bot.path("id").canConvertToLong())return unknown();
            String username=bot.path("username").asText();
            if(!username.matches("[A-Za-z0-9_]{5,32}"))return unknown();
            var chat=call("getChat",Map.of("chat_id",settings.chatId()));
            if(!chat.path("ok").asBoolean())return rejected(chat,false);
            var value=chat.path("result");String type=value.path("type").asText();
            boolean same=settings.chatId().startsWith("@")
                    ?settings.chatId().substring(1).equalsIgnoreCase(value.path("username").asText())
                    :settings.chatId().equals(value.path("id").asText());
            if(!same||!java.util.Set.of("private","group","supergroup","channel").contains(type))return unknown();
            return result("VERIFIED",username,type,"봇 인증과 대화방 조회가 확인됐습니다. 발송 권한·실제 수신은 테스트 메시지로 확인하세요.");
        }catch(InterruptedException e){Thread.currentThread().interrupt();return unknown();}
        catch(Exception e){return unknown();}
    }
    private JsonNode call(String method,Map<String,String> body)throws Exception {
        var request=HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot"+settings.botToken()+"/"+method))
                .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofString());
        var value=mapper.readTree(response.body());
        if(response.statusCode()!=200&&value.path("ok").asBoolean())throw new IllegalStateException();
        return value;
    }
    private Check rejected(JsonNode response,boolean token){
        int code=response.path("error_code").asInt();
        if(code==429)return result("RATE_LIMIT",null,null,"요청이 많습니다. 잠시 후 다시 확인하세요.");
        if(code==401)return result("AUTH_FAILED",null,null,"봇 토큰이 유효하지 않습니다. 서버 설정을 확인하세요.");
        if(!token&&(code==400||code==403))return result("CHAT_UNAVAILABLE",null,null,"대화방 ID와 봇 참여 여부를 확인하세요. 개인 대화는 봇에게 먼저 /start를 보내세요.");
        return unknown();
    }
    private Check unknown(){return result("UNCONFIRMED",null,null,"연결을 확인하지 못했습니다. 네트워크와 서버 설정을 확인한 후 다시 시도하세요.");}
    private Check result(String state,String username,String type,String message){return new Check(state,username,type,settings.enabled(),message);}
}
