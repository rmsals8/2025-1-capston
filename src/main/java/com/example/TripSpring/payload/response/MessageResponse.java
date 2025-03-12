package com.example.TripSpring.payload.response;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
    private String status;
    private Object data;

    public MessageResponse(String message) {
        this.message = message;
        this.status = "success";
    }

    public MessageResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }
}