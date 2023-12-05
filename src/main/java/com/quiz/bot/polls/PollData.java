package com.quiz.bot.polls;

import lombok.Data;

@Data
public class PollData {
    private String question;
    private String[] options;
    private String correctAnswer;
}