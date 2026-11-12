package com.crimson.auth.payment;

import com.crimson.auth.config.TicketingProperties;
import com.crimson.auth.exception.TicketingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {
    private final Map<String, ChargeResult> chargesByIdempotencyKey = new ConcurrentHashMap<>();
    private final Map<String, ChargeResult> chargesByRef = new ConcurrentHashMap<>();
    private final Map<String, String> refunds = new ConcurrentHashMap<>();
    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public MockPaymentGateway(TicketingProperties props, ObjectMapper objectMapper) {
        Assert.hasText(props.webhookSecret(), "app.ticketing.webhook-secret must be set");
        this.secret = props.webhookSecret().getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public ChargeResult charge(ChargeRequest req) {
        return chargesByIdempotencyKey.computeIfAbsent(req.idempotencyKey(), k -> {
            PaymentOutcome outcome = "fail".equals(req.paymentToken()) ? PaymentOutcome.FAILED
                    : "pending".equals(req.paymentToken()) ? PaymentOutcome.PENDING : PaymentOutcome.SUCCEEDED;
            ChargeResult result = new ChargeResult("mock_" + req.paymentId(), outcome,
                    outcome == PaymentOutcome.FAILED ? "Card declined (mock)" : null);
            chargesByRef.put(result.providerRef(), result);
            return result;
        });
    }

    @Override
    public ChargeResult fetchStatus(String providerRef) {
        ChargeResult r = chargesByRef.get(providerRef);
        if (r == null) {
            throw new PaymentGatewayException("Unknown charge " + providerRef);
        }
        return r;
    }

    @Override
    public String refund(RefundRequest req) {
        return refunds.computeIfAbsent(req.idempotencyKey(), k -> "mock_refund_" + req.providerRef());
    }

    @Override
    public WebhookEvent parseWebhook(String rawBody, String signature) {
        if (signature == null || !MessageDigest.isEqual(
                sign(rawBody).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw TicketingException.unauthorized("Invalid webhook signature");
        }
        try {
            WebhookEvent ev = objectMapper.readValue(rawBody, WebhookEvent.class);
            if (ev.eventId() == null || ev.outcome() == null) {
                throw TicketingException.badRequest("Webhook is missing eventId/outcome");
            }
            return ev;
        } catch (TicketingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw TicketingException.badRequest("Unparseable webhook body");
        }
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

