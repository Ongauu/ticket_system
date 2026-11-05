package com.crimson.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ticketing")
public record TicketingProperties(
        @DefaultValue("10m") Duration holdDuration,
        @DefaultValue("10m") Duration paymentWindow,
        @DefaultValue("15m") Duration paymentExpiryGrace,
        @DefaultValue("2m") Duration unackedPaymentTimeout,
        @DefaultValue("1m") Duration reconcileAfter,
        @DefaultValue("6") int maxTicketsPerReservation,
        @DefaultValue("200") int jobBatchSize,
        @DefaultValue("30") int rateLimitPerMinute,
        String ticketSigningSecret,
        String webhookSecret
) {}
