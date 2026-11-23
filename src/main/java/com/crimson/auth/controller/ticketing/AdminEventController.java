package com.crimson.auth.controller.ticketing;

import com.crimson.auth.dto.request.CreateEventRequest;
import com.crimson.auth.dto.response.EventResponse;
import com.crimson.auth.service.TickectingService.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable Long id){
        return ResponseEntity.ok(eventService.publish(id));
    }
}
