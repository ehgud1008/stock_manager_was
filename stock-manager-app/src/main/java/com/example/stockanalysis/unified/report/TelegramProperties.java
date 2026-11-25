package com.example.stockanalysis.unified.report;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("unified.telegram")
public record TelegramProperties(@DefaultValue("false") boolean enabled, @DefaultValue("") String botToken,
                                  @DefaultValue("") String chatId, @DefaultValue("") String publicBaseUrl) {
    public TelegramProperties {
        if (enabled && (botToken==null || !botToken.matches("[0-9]+:[A-Za-z0-9_-]+")
                || chatId==null || chatId.length()>100 || !chatId.matches("-?[0-9]+|@[A-Za-z0-9_]{5,}")))
            throw new IllegalArgumentException("Telegram token and destination must be configured");
        if (publicBaseUrl!=null && !publicBaseUrl.isBlank()) {
            var uri=java.net.URI.create(publicBaseUrl);
            if (publicBaseUrl.length()>512 || !"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
                throw new IllegalArgumentException("Report public URL must be HTTPS without credentials, query or fragment");
        }
    }
    @Override public String toString() { return "TelegramProperties[enabled="+enabled+", credentials=REDACTED]"; }
}
