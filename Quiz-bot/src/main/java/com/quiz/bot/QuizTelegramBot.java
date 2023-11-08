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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
    private final Integer threadId;

    private boolean isActive = true;

    public QuizTelegramBot(BotConfig config) {
        this.config = config; // Присваиваем переданную конфигурацию полю класса
        this.threadId = config.getMessageThreadId();
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
                sendPoll(update.getMessage().getChatId(), threadId); // Отправка опроса
            }
        }
    }

    private void handleTextMessage(Message message) {
        if (message == null) {
            log.error("Передано пустое сообщение.");
            return;
        }

        long chatId = message.getChatId();
        Integer threadId = message.getMessageThreadId();
        if (threadId == null) {
            threadId = config.getMessageThreadId();
        }
        String receivedMessage = message.getText();
        if (receivedMessage == null) {
            log.error("Переданное сообщение не содержит текст.");
            return;
        }

        receivedMessage = receivedMessage.toLowerCase();

        String botName = getBotUsername();
        if (botName == null) {
            log.error("Пустое имя бота.");
            return;
        }

        botName = botName.toLowerCase();

        if (message.getReplyToMessage() != null) {
            message.getReplyToMessage().getFrom().getFirstName();
        }

        if (receivedMessage.equals("/start") || receivedMessage.equals("/start@" + botName)) {
            currentQuestionIndex = 0;
            sendPoll(chatId, threadId);
            log.info("Бот запущен для пользователя: " + message.getFrom().getFirstName());
        } else if (receivedMessage.equals("/help") || receivedMessage.equals("/help@" + botName)) {
            sendHelpText(chatId, threadId);
            log.info("Отправлена справка пользователю: " + message.getFrom().getFirstName());
        } else if (receivedMessage.equals("/exit") || receivedMessage.equals("/exit@" + botName)) {
            isActive = false;
            sendExitMessage(chatId,threadId);
            log.info("Пользователь " + message.getFrom().getFirstName() + " вышел из режима бота");
        } else if (receivedMessage.equals("/next") || receivedMessage.equals("/next@" + botName)) {
            sendCorrectAnswerAndNextQuestion(chatId, threadId);
            log.info("Пользователь " + message.getFrom().getFirstName() + " нажал кнопку '/next'");
        }
    }


    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        int threadId = callbackQuery.getMessage().getMessageThreadId();
        String receivedMessage = callbackQuery.getData();
        String userName = callbackQuery.getFrom().getFirstName();
        String replyUserName = null;

        if (callbackQuery.getMessage().getReplyToMessage() != null) {
            replyUserName = callbackQuery.getMessage().getReplyToMessage().getFrom().getFirstName();
        }
        log.info("Обработка callback query от пользователя: " + userName);
        botAnswerUtils(receivedMessage, chatId, threadId, userName, callbackQuery.getMessage().getMessageId(), replyUserName);

        if ("/next".equals(receivedMessage)) {
            sendCorrectAnswerAndNextQuestion(chatId, threadId);
            log.info("Пользователь " + userName + " нажал кнопку '/next'");
        }
        log.info("Обработка callback query от пользователя " + userName + " завершена");
    }

    private void botAnswerUtils(String receivedMessage, long chatId, int threadId, String userName, long messageId, String replyUserName) {
        String botName = getBotUsername().toLowerCase();

        // Проверяем, содержит ли receivedMessage имя бота (в нижнем регистре)
        if (receivedMessage.equals("/start") || receivedMessage.equals("/start@" + botName)) {
            startBot(chatId, threadId, userName);
            log.info("Бот запущен для пользователя: " + userName);
        } else if (receivedMessage.equals("/help") || receivedMessage.equals("/help@" + botName)) {
            sendHelpText(chatId, threadId);
            log.info("Отправлена справка пользователю: " + userName);
        } else if (receivedMessage.equals("/exit") || receivedMessage.equals("/exit@" + botName)) {
            sendExitMessage(chatId, threadId);
            log.info("Пользователь " + userName + " вышел из режима");
        }

    }

    private void startBot(long chatId, int threadId, String userName) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setMessageThreadId(threadId);
        message.setText("Привет, " + userName + "! Я квиз бот.'");
        message.setReplyMarkup(keyboardMarkup); // Используем предварительно созданный экземпляр клавиатуры

        try {
            execute(message); // Отправляем сообщение
            log.info("Бот запущен. Ответ отправлен"); // Логируем успешную отправку
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }

    private void sendPoll(long chatId, int threadId) {
        PollData[] polls = pollReader.readPolls();

        if (polls != null && polls.length > currentQuestionIndex) {
            PollData pollData = polls[currentQuestionIndex];
            SendPoll poll = getSendPoll(chatId, threadId, pollData, polls);

            try {
                Message message = execute(poll);
                log.info("Отправлен опрос с ID сообщения: ID: " + message.getMessageId());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else {
            log.error("Опросы не найдены или произошла ошибка при чтении опросов из JSON.");
            currentQuestionIndex = 0;
        }
    }

    private SendPoll getSendPoll(long chatId, int threadId, PollData pollData, PollData[] polls) {
        SendPoll poll = new SendPoll();
        poll.setChatId(chatId);
        poll.setMessageThreadId(threadId);
        poll.setQuestion(pollData.getQuestion());
        poll.setOptions(Arrays.asList(pollData.getOptions()));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(); // Изменили тип клавиатуры

        List<List<InlineKeyboardButton>> keyboard = getLists(polls);
        markup.setKeyboard(keyboard);

        poll.setReplyMarkup(markup);
        return poll;
    }

    private List<List<InlineKeyboardButton>> getLists(PollData[] polls) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        // Создаем инлайн кнопку "Помощь" с callback-данными "/help" и добавляем её в первую строку
        InlineKeyboardButton helpButton = new InlineKeyboardButton();
        helpButton.setText("Помощь");
        helpButton.setCallbackData("/help");
        row1.add(helpButton);

        // Создаем инлайн кнопку "Выход" с callback-данными "/exit" и добавляем её в первую строку
        InlineKeyboardButton exitButton = new InlineKeyboardButton();
        exitButton.setText("Выход");
        exitButton.setCallbackData("/exit");
        row1.add(exitButton);

        // Если есть ещё вопросы, создаем кнопку "Следующий вопрос" и устанавливаем для неё callback-данные "/next" во второй строке
        if (currentQuestionIndex < polls.length - 1) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("Следующий вопрос");
            nextButton.setCallbackData("/next");
            row2.add(nextButton);
        }

        // Добавляем обе строки в клавиатуру
        keyboard.add(row1);
        keyboard.add(row2);

        return keyboard;
    }




    private void sendHelpText(long chatId, int threadId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setMessageThreadId(threadId);
        message.setText(BotCommands.HELP_TEXT); // Устанавливаем текст справки из предопределенных констант

        try {
            execute(message); // Отправляем сообщение
            log.info("Отправлен ответ"); // Логируем успешную отправку
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }

    private void sendCorrectAnswerAndNextQuestion(long chatId, int threadId) {
        PollData[] polls = pollReader.readPolls();
        if (polls != null && currentQuestionIndex < polls.length) {
            SendMessage correctAnswerMessage = getSendMessage(chatId,threadId, polls);

            try {
                execute(correctAnswerMessage);

                if (currentQuestionIndex < polls.length - 1) {
                    currentQuestionIndex++;
                    sendPoll(chatId, threadId); // Отправляем следующий вопрос
                } else {
                    // Достигнут последний вопрос, убираем все кнопки и деактивируем бота
                    SendMessage lastQuestionMessage = getSendMessage(chatId, threadId);

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

    private SendMessage getSendMessage(long chatId, int threadId, PollData[] polls) {
        PollData pollData = polls[currentQuestionIndex];
        String correctAnswer = pollData.getCorrectAnswer();

        String escapedCorrectAnswer = escapeSpecialCharacters(correctAnswer);

        SendMessage correctAnswerMessage = new SendMessage();
        correctAnswerMessage.setChatId(chatId);
        correctAnswerMessage.setMessageThreadId(threadId);
        correctAnswerMessage.setText("Правильный ответ был:\n ||" + escapedCorrectAnswer + "||");
        correctAnswerMessage.setParseMode("MarkdownV2");

        return correctAnswerMessage;
    }

    private static SendMessage getSendMessage(long chatId, int threadId) {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);

        SendMessage lastQuestionMessage = new SendMessage();
        lastQuestionMessage.setChatId(chatId);
        lastQuestionMessage.setMessageThreadId(threadId);
        lastQuestionMessage.setText("Это был последний вопрос. Бот уходит в режим ожидания. Нажмите /start, чтобы начать заново.");
        lastQuestionMessage.setReplyMarkup(keyboardRemove);
        return lastQuestionMessage;
    }

    private void sendExitMessage(long chatId, int threadId) {
        SendMessage exitMessage = new SendMessage();
        exitMessage.setChatId(chatId);
        exitMessage.setMessageThreadId(threadId);
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