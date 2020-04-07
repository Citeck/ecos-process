package ru.citeck.ecos.process.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Getter
public class BadRequestException extends RuntimeException {

    private String message;

    public BadRequestException(String message) {
        super("Request is wrong: " + message);
        this.message = message;
    }
}
