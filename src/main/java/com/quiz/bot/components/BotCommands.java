package com.quiz.bot.components;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

public interface BotCommands {

    List<BotCommand> LIST_OF_COMMANDS = List.of(
            new BotCommand("/start" , "Запуск бота"),
            new BotCommand("/help", "Информация"),
            new BotCommand("/exit", "Выход")
    );

    String HELP_TEXT = "Этот бот написан на Spring. " +
            "Для общения используйте команды:\n\n" +
            "/start - Запустить бота\n" +
            "/help - Помощь\n" +
            "/exit - Выход";
}