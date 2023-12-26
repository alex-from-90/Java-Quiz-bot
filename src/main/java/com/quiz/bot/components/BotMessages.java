package com.quiz.bot.components;

public class BotMessages {
    public static final String START_MESSAGE = "Привет, %s!\nЯ квиз бот.";
    public static final String IN_USE_TEXT = "Бот уже запущен\n /help для помощи\n /next - следующий вопрос";
    public static final String SLEEP_MESSAGE = "Бот спит\nРазбудить?\n /start";
    public static final String HELP_TEXT = """
            Этот бот написан на SpringBoot.
            Для общения используйте команды:

            /start - Запустить бота
            /next - Следующий вопрос
            /restart - перезапуск вопросов
            /help - Помощь
            /exit - Выход

            Связаться с разработчиком
            @Site_n_ru

            Угостить разработчика кофе ;)
            https://pay.cloudtips.ru/p/bb25a417
            """;
    public static final String EXIT_TEXT = """
            Вы вышли из режима.
            Для продолжения нажмите /start.
            Если вам понравился бот,
            вы всегда можете поддержать автора.

            Угостить разработчика кофе ;)
            https://pay.cloudtips.ru/p/bb25a417
            """;
    public static final String LAST_QUESTION_TEXT = """
            Это был последний вопрос.
            Бот уходит в режим ожидания.
            Нажмите /start, чтобы начать заново.
            Если вам понравился бот,
            вы всегда можете поддержать автора и угостить разработчика кофе ;)
            
            https://pay.cloudtips.ru/p/bb25a417
            """;
}

