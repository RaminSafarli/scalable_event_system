package data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TicketRequest {
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("event_id")
    private Long eventId;
}
