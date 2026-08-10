package com.epam.gym.messaging;

import com.epam.gym.dto.client.ActionType;
import com.epam.gym.dto.client.WorkloadRequest;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadMessageProducerTest {

    private static final String QUEUE = "workload.queue";

    @Mock private JmsTemplate jmsTemplate;

    private WorkloadMessageProducer producer;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private WorkloadRequest sampleRequest() {
        return WorkloadRequest.builder()
                .trainerUsername("Bruce.Wayne")
                .trainerFirstName("Bruce")
                .trainerLastName("Wayne")
                .isActive(true)
                .trainingDate(LocalDate.of(2024, 1, 10))
                .trainingDuration(45)
                .actionType(ActionType.ADD)
                .build();
    }

    @Test
    void sendWorkload_sendsToConfiguredQueue() {
        producer = new WorkloadMessageProducer(jmsTemplate, QUEUE);
        WorkloadRequest request = sampleRequest();

        producer.sendWorkload(request);

        verify(jmsTemplate).convertAndSend(eq(QUEUE), eq(request), any(MessagePostProcessor.class));
    }

    @Test
    void sendWorkload_setsTransactionIdPropertyFromMdc() throws JMSException {
        producer = new WorkloadMessageProducer(jmsTemplate, QUEUE);
        MDC.put("transactionId", "tx-123");
        WorkloadRequest request = sampleRequest();

        producer.sendWorkload(request);

        // capture the post-processor and apply it to a mock message to inspect properties
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq(QUEUE), eq(request), captor.capture());

        Message mockMessage = mock(Message.class);
        captor.getValue().postProcessMessage(mockMessage);

        verify(mockMessage).setStringProperty("transactionId", "tx-123");
    }

    @Test
    void sendWorkload_withoutTransactionId_doesNotSetProperty() throws JMSException {
        producer = new WorkloadMessageProducer(jmsTemplate, QUEUE);
        // no MDC transactionId, no servlet request -> no auth header
        WorkloadRequest request = sampleRequest();

        producer.sendWorkload(request);

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq(QUEUE), eq(request), captor.capture());

        Message mockMessage = mock(Message.class);
        captor.getValue().postProcessMessage(mockMessage);

        // neither Authorization nor transactionId set
        verify(mockMessage, never()).setStringProperty(eq("transactionId"), any());
        verify(mockMessage, never()).setStringProperty(eq("Authorization"), any());
    }

    @Test
    void sendWorkload_doesNotThrow_whenJmsSucceeds() {
        producer = new WorkloadMessageProducer(jmsTemplate, QUEUE);
        WorkloadRequest request = sampleRequest();

        // just ensure no exception bubbles up
        producer.sendWorkload(request);

        verify(jmsTemplate, times(1))
                .convertAndSend(eq(QUEUE), eq(request), any(MessagePostProcessor.class));
    }
}