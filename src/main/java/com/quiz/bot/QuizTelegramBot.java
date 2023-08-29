package com.quiz.bot;

import com.quiz.bot.components.BotCommands;
import com.quiz.bot.components.Buttons;
import com.quiz.bot.coonfig.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class QuizTelegramBot extends TelegramLongPollingBot implements BotCommands {

    final BotConfig config;
    private final InlineKeyboardMarkup keyboardMarkup = Buttons.inlineMarkup();
    private final PollReader pollReader = new PollReader();
    private int currentQuestionIndex = 0;

    private boolean isActive = true;

    public QuizTelegramBot(BotConfig config) {
        this.config = config; // Присваиваем переданную конфигурацию полю класса

        try {
            // Попытка выполнить команду SetMyCommands с переданным списком команд и настройками по умолчанию
            this.execute(new SetMyCommands(LIST_OF_COMMANDS, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (!isActive) {
            handleInactive(update); // Если бот неактивен, обрабатываем обновление соответствующим образом
            return;
        }

        if (update.hasMessage()) {
            handleTextMessage(update.getMessage()); // Если обновление содержит сообщение, обрабатываем текстовое сообщение
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery()); // Если обновление содержит callback query, обрабатываем его
        }
    }

    private void handleInactive(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String originalReceivedMessage = update.getMessage().getText();
            String receivedMessage = originalReceivedMessage.toLowerCase();
            String botName = getBotUsername().toLowerCase();

            if (receivedMessage.equals("/start") || receivedMessage.equals("/start@" + botName)) {
                isActive = true; // Активация бота
                sendPoll(update.getMessage().getChatId()); // Отправка опроса
            }
        }
    }

    private void handleTextMessage(Message message) {
        long chatId = message.getChatId();
        String receivedMessage = message.getText().toLowerCase();

        // Извлекаем имя бота
        String botName = getBotUsername().toLowerCase();

        if (message.getReplyToMessage() != null) {
            String replyUserName = message.getReplyToMessage().getFrom().getFirstName();
        }

        if (receivedMessage.equals("/start") || receivedMessage.equals("/start@" + botName)) {
            currentQuestionIndex = 0;
            sendPoll(chatId);
            log.info("Бот запущен для пользователя: " + message.getFrom().getFirstName());
        } else if (receivedMessage.equals("/help") || receivedMessage.equals("/help@" + botName)) {
            sendHelpText(chatId);
            log.info("Отправлена справка пользователю: " + message.getFrom().getFirstName());

        } else if (receivedMessage.equals("/exit") || receivedMessage.equals("/exit@" + botName)) {
            isActive = false;
            sendExitMessage(chatId);
            log.info("Пользователь " + message.getFrom().getFirstName() + " вышел из режима бота");

        } else if (receivedMessage.equals("/next")) { // Check for /next command
            sendCorrectAnswerAndNextQuestion(chatId);
            log.info("Пользователь " + message.getFrom().getFirstName() + " нажал кнопку '/next'");

        } else {
            // Handle other commands or messages
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId(); // Получаем ID чата
        String receivedMessage = callbackQuery.getData(); // Получаем данные из callback query
        String userName = callbackQuery.getFrom().getFirstName(); // Получаем имя пользователя
        String replyUserName = null;

        if (callbackQuery.getMessage().getReplyToMessage() != null) {
            replyUserName = callbackQuery.getMessage().getReplyToMessage().getFrom().getFirstName(); // Получаем имя отправителя оригинального сообщения, на которое дан ответ
        }
        log.info("Обработка callback query от пользователя: " + userName);
        botAnswerUtils(receivedMessage, chatId, userName, callbackQuery.getMessage().getMessageId(), replyUserName); // Вызываем вспомогательный метод для обработки ответа

        if ("/next".equals(receivedMessage)) {
            sendCorrectAnswerAndNextQuestion(chatId); // Если получен ответ "/next", отправляем следующий вопрос и правильный ответ
            log.info("Пользователь " + userName + " нажал кнопку '/next'");
        }
        log.info("Обработка callback query от пользователя " + userName + " завершена");
    }

    private void botAnswerUtils(String receivedMessage, long chatId, String userName, long messageId, String replyUserName) {
        String botName = getBotUsername().toLowerCase();

        // Проверяем, содержит ли receivedMessage имя бота (в нижнем регистре)
        if (receivedMessage.equals("/start") || receivedMessage.equals("/start@" + botName)) {
            startBot(chatId, userName);
            log.info("Бот запущен для пользователя: " + userName);
        } else if (receivedMessage.equals("/help") || receivedMessage.equals("/help@" + botName)) {
            sendHelpText(chatId);
            log.info("Отправлена справка пользователю: " + userName);
        } else if (receivedMessage.equals("/exit") || receivedMessage.equals("/exit@" + botName)) {
            sendExitMessage(chatId);
            log.info("Пользователь " + userName + " вышел из режима");
        }

    }

    private void startBot(long chatId, String userName) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Привет, " + userName + "! Я квиз бот.'");
        message.setReplyMarkup(keyboardMarkup); // Используем предварительно созданный экземпляр клавиатуры

        try {
            execute(message); // Отправляем сообщение
            log.info("Бот запущен. Ответ отправлен"); // Логируем успешную отправку
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }

    private void sendPoll(long chatId) {
        PollData[] polls = pollReader.readPolls(); // Читаем список опросов
        if (polls != null && polls.length > currentQuestionIndex) {
            PollData pollData = polls[currentQuestionIndex]; // Получаем данные текущего вопроса
            SendPoll poll = new SendPoll();
            poll.setChatId(chatId);
            poll.setQuestion(pollData.getQuestion()); // Устанавливаем вопрос
            poll.setOptions(Arrays.asList(pollData.getOptions())); // Устанавливаем варианты ответов

            List<KeyboardRow> keyboard = new ArrayList<>();
            KeyboardRow row = new KeyboardRow();

            // Добавляем кнопку "Next", если есть следующий вопрос
            if (currentQuestionIndex < polls.length - 1) {
                row.add(new KeyboardButton("/next"));
            }

            keyboard.add(row);

            ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
            markup.setKeyboard(keyboard);
            markup.setResizeKeyboard(true);

            poll.setReplyMarkup(markup); // Устанавливаем клавиатуру с кнопкой

            try {
                Message message = execute(poll); // Отправляем опрос
                log.info("Отправлен опрос с ID сообщения: ID: " + message.getMessageId());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else {
            log.error("Опросы не найдены или произошла ошибка при чтении опросов из JSON.");
            currentQuestionIndex = 0; // Сбрасываем индекс вопроса, если больше нет доступных опросов
        }
    }


    private void sendHelpText(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(BotCommands.HELP_TEXT); // Устанавливаем текст справки из предопределенных констант

        try {
            execute(message); // Отправляем сообщение
            log.info("Отправлен ответ"); // Логируем успешную отправку
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }

    private void sendCorrectAnswerAndNextQuestion(long chatId) {
        PollData[] polls = pollReader.readPolls();
        if (polls != null && currentQuestionIndex < polls.length) {
            SendMessage correctAnswerMessage = getSendMessage(chatId, polls);

            try {
                execute(correctAnswerMessage);

                if (currentQuestionIndex < polls.length - 1) {
                    currentQuestionIndex++;
                    sendPoll(chatId); // Отправляем следующий вопрос
                } else {
                    // Достигнут последний вопрос, убираем все кнопки и деактивируем бота
                    SendMessage lastQuestionMessage = getSendMessage(chatId);

                    isActive = false; // Деактивируем бота после отправки финального сообщения
                    execute(lastQuestionMessage); // Отправляем сообщение с убранными кнопками
                    currentQuestionIndex = 0; // Сбрасываем индекс вопроса, когда вопросы закончились
                }
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    private String escapeSpecialCharacters(String input) {
        String[] specialCharacters = {"|", ".", "~", "(", ")", "'"}; // Добавьте другие символы при необходимости

        for (String character : specialCharacters) {
            input = input.replace(character, "\\" + character);
        }

        return input;
    }

    private SendMessage getSendMessage(long chatId, PollData[] polls) {
        PollData pollData = polls[currentQuestionIndex];
        String correctAnswer = pollData.getCorrectAnswer();

        String escapedCorrectAnswer = escapeSpecialCharacters(correctAnswer);

        SendMessage correctAnswerMessage = new SendMessage();
        correctAnswerMessage.setChatId(chatId);
        correctAnswerMessage.setText("Правильный ответ был:\n ||" + escapedCorrectAnswer + "||");
        correctAnswerMessage.setParseMode("MarkdownV2");

        return correctAnswerMessage;
    }

    private static SendMessage getSendMessage(long chatId) {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);

        SendMessage lastQuestionMessage = new SendMessage();
        lastQuestionMessage.setChatId(chatId);
        lastQuestionMessage.setText("Это был последний вопрос. Бот уходит в режим ожидания. Нажмите /start, чтобы начать заново.");
        lastQuestionMessage.setReplyMarkup(keyboardRemove);
        return lastQuestionMessage;
    }

    private void sendExitMessage(long chatId) {
        SendMessage exitMessage = new SendMessage();
        exitMessage.setChatId(chatId);
        exitMessage.setText("Вы вышли из режима. Для продолжения нажмите /start.");

        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        exitMessage.setReplyMarkup(keyboardRemove);

        try {
            execute(exitMessage); // Отправляем сообщение
            isActive = false; // Деактивируем бота после отправки сообщения
            currentQuestionIndex = 0; // Сбрасываем индекс вопроса, когда вопросы закончились
            log.info("Пользователь вышел из режима бота");
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }
}
