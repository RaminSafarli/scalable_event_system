package com.codewithramin.ticket.controller;

import data.TicketRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    private final ConnectionFactory factory;
    private final ObjectMapper mapper;
    private final String queueName;

    public TicketController(ObjectMapper mapper,
                            @Value("${app.rabbitmq.host}") String rabbitHost,
                            @Value("${app.rabbitmq.port}") int rabbitPort,
                            @Value("${app.rabbitmq.queue.name}") String queueName) {
        this.mapper = mapper;
        this.queueName = queueName;
        factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setPort(rabbitPort);
    }

    @PostMapping("")
    public ResponseEntity<String> getTicket(@RequestBody TicketRequest ticket) throws IOException {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, false, false, false, null);
            String jsonMessage = mapper.writeValueAsString(ticket);
            System.out.println("Sending request to topic: " + jsonMessage);
            channel.basicPublish("", queueName, null, jsonMessage.getBytes(StandardCharsets.UTF_8));
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.status(202).body("Sent" + " a message to " + queueName);
    }
}
