package com.quiz.bot.coonfig;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("config.properties")
public class BotConfig {
    @Value("${bot.name}") String botName;
    @Value("${bot.token}") String token;
    @Value("${bot.chatId}") String chatId;
    @Value("${bot.messageThreadId}") Integer messageThreadId;
    @Value("${bot.quizURL}") String botQuizUrl;
}
