package com.tenten.zimparks.event;

import com.tenten.zimparks.user.Role;
import com.tenten.zimparks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventStreamController {

    private final UserRepository userRepo;

    /**
     * Each active SSE connection is stored together with the station it belongs to.
     * stationId = null means the client is an admin and receives all events.
     */
    private record EmitterEntry(SseEmitter emitter, String stationId) {}
    private final List<EmitterEntry> entries = new CopyOnWriteArrayList<>();

    /** Client subscribes here */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");

        String stationId = resolveStationId();

        SseEmitter emitter = new SseEmitter(0L); // no timeout
        EmitterEntry entry = new EmitterEntry(emitter, stationId);
        entries.add(entry);
        emitter.onCompletion(() -> entries.remove(entry));
        emitter.onTimeout(()    -> entries.remove(entry));
        emitter.onError(e ->      entries.remove(entry));
        return emitter;
    }

    /** Call this from TransactionService after void approve/reject */
    public void broadcastTxUpdate() { broadcastAll("tx-updated"); }

    /** Call this from CreditNoteService after approve/reject */
    public void broadcastCNUpdate() { broadcastAll("cn-updated"); }

    /**
     * Call this from CashupService after submit or review.
     * Only notifies supervisors whose station matches the operator's station.
     * Admins are excluded — cashup review is a supervisor responsibility.
     * If the operator has no station on record, notifies all supervisors.
     */
    public void broadcastCashupUpdate(String operatorStationId) {
        for (EmitterEntry e : entries) {
            if (e.stationId() == null) continue; // skip admins
            if (operatorStationId == null || operatorStationId.equals(e.stationId())) {
                try { e.emitter().send(SseEmitter.event().name("cashup-updated").data("")); }
                catch (Exception ex) { entries.remove(e); }
            }
        }
    }

    /** Broadcasts an event to every connected client regardless of station. */
    private void broadcastAll(String eventName) {
        for (EmitterEntry e : entries) {
            try { e.emitter().send(SseEmitter.event().name(eventName).data("")); }
            catch (Exception ex) { entries.remove(e); }
        }
    }

    /**
     * Returns the station ID of the currently authenticated user if they are a supervisor,
     * or null for admins/operators. Null entries are skipped in broadcastCashupUpdate.
     */
    private String resolveStationId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username  = principal instanceof UserDetails ud ? ud.getUsername() : principal.toString();
            return userRepo.findByUsername(username)
                    .filter(u -> u.getRole() == Role.SUPERVISOR)
                    .map(u -> u.getStation() != null ? u.getStation().getId() : null)
                    .orElse(null);
        } catch (Exception e) {
            return null; // unauthenticated or lookup failed — treated as admin (receives all)
        }
    }
}
