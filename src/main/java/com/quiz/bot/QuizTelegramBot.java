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
        if (update.hasMessage() && update.getMessage().hasText() && "/start".equals(update.getMessage().getText())) {
            isActive = true; // Активируем бота, так как получено сообщение с текстом "/start"
            sendPoll(update.getMessage().getChatId()); // Отправляем опрос в чат
        }
    }

    private void handleTextMessage(Message message) {
        long chatId = message.getChatId();
        String receivedMessage = message.getText();
        message.getFrom().getFirstName(); // Получаем имя отправителя сообщения

        if (message.getReplyToMessage() != null) {
            message.getReplyToMessage().getFrom().getFirstName(); // Получаем имя отправителя сообщения, на которое было дано ответное сообщение
        }

        switch (receivedMessage) {
            case "/start" -> {
                currentQuestionIndex = 0; // Сбрасываем индекс текущего вопроса
                sendPoll(chatId); // Отправляем опрос в чат
            }
            case "/help" -> sendHelpText(chatId); // Отправляем текст помощи в чат
            case "/next" -> sendCorrectAnswerAndNextQuestion(chatId); // Отправляем правильный ответ и следующий вопрос
            case "/exit" -> {
                isActive = false; // Деактивируем бота
                sendExitMessage(chatId); // Отправляем сообщение о завершении работы бота
            }
            default -> {
            }
            // Действия по умолчанию, если не совпало ни одно из условий
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

        botAnswerUtils(receivedMessage, chatId, userName, callbackQuery.getMessage().getMessageId(), replyUserName); // Вызываем вспомогательный метод для обработки ответа

        if ("/next".equals(receivedMessage)) {
            sendCorrectAnswerAndNextQuestion(chatId); // Если получен ответ "/next", отправляем следующий вопрос и правильный ответ
        }
    }

    private void botAnswerUtils(String receivedMessage, long chatId, String userName, long messageId, String replyUserName) {
        switch (receivedMessage) {
            case "/start" -> startBot(chatId, userName); // Запускаем бота при команде "/start"
            case "/help" -> sendHelpText(chatId); // Отправляем текст помощи при команде "/help"
            case "/exit" ->
                    sendExitMessage(chatId); // Отправляем сообщение о завершении работы бота при команде "/exit"

            // Обработка других команд
            default -> {
            }
        }
    }

    private void startBot(long chatId, String userName) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Привет, " + userName + "! Я квиз бот.'");
        message.setReplyMarkup(keyboardMarkup); // Используем предварительно созданный экземпляр клавиатуры

        try {
            execute(message); // Отправляем сообщение
            log.info("Reply sent"); // Логируем успешную отправку
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
                log.info("Poll sent with message ID: " + message.getMessageId());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else {
            log.error("No polls found or an error occurred while reading polls from JSON.");
            currentQuestionIndex = 0; // Сбрасываем индекс вопроса, если больше нет доступных опросов
        }
    }


    private void sendHelpText(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(BotCommands.HELP_TEXT); // Устанавливаем текст справки из предопределенных констант

        try {
            execute(message); // Отправляем сообщение
            log.info("Reply sent"); // Логируем успешную отправку
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }

    private void sendCorrectAnswerAndNextQuestion(long chatId) {
        PollData[] polls = pollReader.readPolls();
        if (polls != null && currentQuestionIndex < polls.length) {
            PollData pollData = polls[currentQuestionIndex];
            String correctAnswer = pollData.getCorrectAnswer();

            SendMessage correctAnswerMessage = new SendMessage();
            correctAnswerMessage.setChatId(chatId);
            correctAnswerMessage.setText("Правильный ответ был:\n ||" + correctAnswer + "||");
            correctAnswerMessage.setParseMode("MarkdownV2");

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
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }
}
