package com.crimson.auth.exception;

import org.springframework.http.HttpStatus;

public class TicketingException extends RuntimeException  {
    private final HttpStatus status;

    private  TicketingException(HttpStatus status, String message) {
        super(message, null, false, false);
        this.status = status;
    }

    public HttpStatus getStatus(){
        return status;
    }

    public static TicketingException badRequest(String m) {
        return new TicketingException(HttpStatus.BAD_REQUEST, m);
    }

    public static TicketingException unauthorized(String m){
        return new TicketingException(HttpStatus.UNAUTHORIZED, m);
    }

    public static TicketingException notFound(String m) {
        return new TicketingException(HttpStatus.NOT_FOUND, m);
    }

    public static TicketingException conflict(String m) {
        return new TicketingException(HttpStatus.CONFLICT, m);
    }

    public static TicketingException gone(String m) {
        return new TicketingException(HttpStatus.GONE, m);
    }

    public static TicketingException unprocessable(String m) {
        return new TicketingException(HttpStatus.valueOf(422), m);
    }

    public static TicketingException unavailable(String m) {
        return new TicketingException(HttpStatus.SERVICE_UNAVAILABLE, m);
    }
}
