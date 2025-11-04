package org.processor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static String getEnvOrDefault(String variableName, String defaultValue) {
        String value = System.getenv(variableName);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private static java.sql.Connection dbConnection;
    private static final String DB_HOST = getEnvOrDefault("DB_HOST_ENV", "localhost");
    private static final String DB_USER = getEnvOrDefault("DB_USER_ENV", "postgres");
    private static final String DB_PASSWORD = getEnvOrDefault("DB_PASS_ENV", "mysecretpassword");
    private static final String DB_NAME = getEnvOrDefault("DB_NAME_ENV", "postgres");
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":5432/" + DB_NAME;

    private static final String QUEUE_NAME = getEnvOrDefault("RABBIT_QUEUE_ENV", "ticket_requests");
    private static final String RABBITMQ_HOST = getEnvOrDefault("RABBIT_HOST_ENV", "localhost");
    private static final int RABBITMQ_PORT = Integer.parseInt(getEnvOrDefault("RABBIT_PORT_ENV", "5672"));

    public static void main() throws IOException, TimeoutException, InterruptedException, SQLException {
        dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setPort(RABBITMQ_PORT);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");

            try {
                JSONObject obj = new JSONObject(message);
                int userId = Integer.parseInt(obj.optString("user_id"));
                int eventId = Integer.parseInt(obj.optString("event_id"));

                try {
                    System.out.println(" [i] Processing ticket for user: " + userId + " (Simulating 1s work...)");
                    Thread.sleep(1000); // 1000 milisaniye = 1 saniye
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Thread'i yeniden kesintiye uğrat
                }

                String sql = "INSERT INTO tickets (user_id, event_id) VALUES (?, ?)";

                try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, eventId);
                    pstmt.executeUpdate();

                    System.out.println(" [x] Inserted '" + message + "'");
                    // SADECE BAŞARILIYSA: RabbitMQ'ya "işim bitti" de.
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (SQLException e) {
                    System.err.println("--- DATABASE ERROR ---");
                    e.printStackTrace();
                    System.err.println("----------------------");
                }
            } catch (JSONException e) {
                System.err.println("--- JSON ERROR ---");
                e.printStackTrace();
                System.err.println("----------------------");
            }

        };

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
        });

        // Prevent the JVM from exiting
        while (true) {
            Thread.sleep(1000);
        }

    }
}
