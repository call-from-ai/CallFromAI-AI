package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.exception.RequestIdConflictException;
import com.example.aidatingagentbackend.repository.AiRequestLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:idempotency;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "gemini.api-key=test-key"
})
class RequestIdempotencyServiceTests {

    @Autowired
    private RequestIdempotencyService service;

    @Autowired
    private AiRequestLedgerRepository repository;

    @BeforeEach
    void cleanLedger() {
        repository.deleteAll();
    }

    @Test
    void returnsStoredResponseForSameRequest() {
        ChatRequest request = request("req-1", "hello");
        AtomicInteger executions = new AtomicInteger();

        ChatResponse first = service.execute(request, () -> response("req-1", "first", executions));
        ChatResponse replay = service.execute(request, () -> response("req-1", "second", executions));

        assertThat(first.getReply()).isEqualTo("first");
        assertThat(replay.getReply()).isEqualTo("first");
        assertThat(executions).hasValue(1);
    }

    @Test
    void rejectsSameRequestIdWithDifferentBody() {
        service.execute(request("req-2", "first"), () -> response("req-2", "reply", new AtomicInteger()));

        assertThatThrownBy(() -> service.execute(
                request("req-2", "different"),
                () -> response("req-2", "other", new AtomicInteger())
        )).isInstanceOf(RequestIdConflictException.class)
                .hasMessageContaining("different request body");
    }

    @Test
    void rejectsRequestThatIsStillProcessing() {
        ChatRequest request = request("req-3", "hello");
        service.claim(request);

        assertThatThrownBy(() -> service.claim(request))
                .isInstanceOf(RequestIdConflictException.class)
                .hasMessageContaining("being processed");
    }

    private ChatRequest request(String requestId, String message) {
        ChatRequest request = new ChatRequest();
        request.setRequestId(requestId);
        request.setMessage(message);
        return request;
    }

    private ChatResponse response(String requestId, String reply, AtomicInteger executions) {
        executions.incrementAndGet();
        ChatResponse response = new ChatResponse(reply);
        response.setRequestId(requestId);
        return response;
    }
}
