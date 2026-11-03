package com.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterResponse {
    private Integer id;   // В ответе ReqRes id — число
    private String token;
}