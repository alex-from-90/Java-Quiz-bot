package com.quiz.bot.components;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

public interface BotCommands {

    List<BotCommand> LIST_OF_COMMANDS = List.of(
            new BotCommand("/start", "Запуск бота"),
            new BotCommand("/help", "Информация"),
            new BotCommand("/next", "Следующий вопрос"),
            new BotCommand("/exit", "Выход"),
            new BotCommand("/restart", "Перезапуск вопросов")
    );
}