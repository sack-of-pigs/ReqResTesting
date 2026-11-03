package com.api.models.request;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserRequest {
    private String name;
    private String job;
}