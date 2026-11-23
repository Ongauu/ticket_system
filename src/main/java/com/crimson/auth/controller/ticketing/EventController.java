package com.crimson.auth.controller.ticketing;

import com.crimson.auth.dto.response.EventResponse;
import com.crimson.auth.dto.response.PageResponse;
import com.crimson.auth.service.TickectingService.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofSeconds(2)).cachePublic();

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<PageResponse<EventResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("startsAt"));
        return ResponseEntity.ok().cacheControl(CACHE).body(eventService.listPublished(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CACHE).body(eventService.getPublished(id));
    }
}

