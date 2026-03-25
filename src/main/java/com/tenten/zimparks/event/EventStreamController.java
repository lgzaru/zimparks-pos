package com.tenten.zimparks.event;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/events")
public class EventStreamController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** Client subscribes here */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e ->      emitters.remove(emitter));
        return emitter;
    }

    /** Call this from TransactionService after void approve/reject */
    public void broadcastTxUpdate() { broadcast("tx-updated"); }

    /** Call this from CreditNoteService after approve/reject */
    public void broadcastCNUpdate() { broadcast("cn-updated"); }

    private void broadcast(String eventName) {
        for (SseEmitter e : emitters) {
            try { e.send(SseEmitter.event().name(eventName).data("")); }
            catch (Exception ex) { emitters.remove(e); }
        }
    }
}
