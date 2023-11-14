package com.quiz.bot.poll;

import lombok.Data;

@Data
public class PollData {
    private String question;
    private String[] options;
    private String correctAnswer;
}