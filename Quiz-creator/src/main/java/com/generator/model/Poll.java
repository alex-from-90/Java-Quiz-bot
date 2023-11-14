package com.generator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Poll {
    private String question;
    private List<String> options;
    private String correctAnswer;

    // Конструктор без аргументов
    public Poll() {
    }

    @JsonCreator
    public Poll(@JsonProperty("question") String question,
                @JsonProperty("options") List<String> options,
                @JsonProperty("correctAnswer") String correctAnswer) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }
}
