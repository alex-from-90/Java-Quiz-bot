package com.quiz.bot.components;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

public interface BotCommands {

    List<BotCommand> LIST_OF_COMMANDS = List.of(
            new BotCommand("/start", "Запуск бота"),
            new BotCommand("/help", "Информация"),
            new BotCommand("/next", "Следующий вопрос"),
            new BotCommand("/exit", "Выход")
    );

    String HELP_TEXT = "Этот бот написан на SpringBoot. " +
            "Для общения используйте команды:\n\n" +
            "/start - Запустить бота\n" +
            "/next - Следующий вопрос\n" +
            "/help - Помощь\n" +
            "/exit - Выход\n"+
            "Присоединится к разработке\n"+
            "@Site_n_ru\n" +
            "Угостить меня кофе ;)\n" +
            "https://pay.cloudtips.ru/p/bb25a417";


}