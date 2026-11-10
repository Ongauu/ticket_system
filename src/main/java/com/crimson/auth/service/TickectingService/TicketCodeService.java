package com.crimson.auth.service.TickectingService;

import com.crimson.auth.config.TicketingProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class TicketCodeService {
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private final byte[] key;

    public TicketCodeService(TicketingProperties props){
        Assert.hasText(props.ticketSigningSecret(),"app.ticketing.ticket-signing-secret must be set");
        this.key = props.ticketSigningSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String newCode(){
        byte[] b = new byte[16];
        RNG.nextBytes(b);
        return B64.encodeToString(b);
    }

    public String qrPayload(String code){
        return code + "." + sign(code);
    }

    public Optional<String> verify(String qrPayload){
        int dot = qrPayload.lastIndexOf('.');
        if(dot < 0 || dot == qrPayload.length() - 1){
            return Optional.empty();
        }
        String code = qrPayload.substring(0, dot);
        String sig = qrPayload.substring(dot + 1);
        boolean ok = MessageDigest.isEqual(sign(code).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
        return ok ? Optional.of(code) : Optional.empty();
    }

    private String sign(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return B64.encodeToString(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

}
