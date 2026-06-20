package com.triptrekker.modules.notification.internal.channel.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseConnectionRegistry {

    private final ConcurrentHashMap<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String recipientId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.computeIfAbsent(recipientId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(recipientId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());

        return emitter;
    }

    public void send(String recipientId, Object data) {
        List<SseEmitter> recipientEmitters = emitters.getOrDefault(recipientId, List.of());
        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : recipientEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        dead.forEach(emitter -> removeEmitter(recipientId, emitter));
    }

    private void removeEmitter(String recipientId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(recipientId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(recipientId);
            }
        }
    }
}
