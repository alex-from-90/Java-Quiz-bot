package com.quiz.bot.bot;

import com.quiz.bot.components.BotCommands;
import com.quiz.bot.components.BotMessages;
import com.quiz.bot.components.Buttons;
import com.quiz.bot.coonfig.BotConfig;
import com.quiz.bot.polls.PollData;
import com.quiz.bot.polls.PollReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;

@Slf4j
@Component
public class QuizTelegramBot extends TelegramLongPollingBot implements BotCommands {

    private final BotConfig config;
    private final InlineKeyboardMarkup keyboardMarkup = Buttons.inlineMarkup();

    private final PollReader pollReader; // Поле класса для PollReader
    private int currentQuestionIndex = 0;
    private Integer threadId;
    private boolean isActive = true;


    public QuizTelegramBot(BotConfig config) {
        super(config.getToken());
        this.config = config;
        this.threadId = config.getMessageThreadId();
        this.pollReader = new PollReader(config.getBotQuizUrl());
        try {
            this.execute(new SetMyCommands(LIST_OF_COMMANDS, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
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
            String userName = update.getMessage().getFrom().getFirstName();
            currentQuestionIndex = 0;

            long chatId = update.getMessage().getChatId();

            if (receivedMessage.equals("/start") || receivedMessage.equals("/start@" + botName)) {
                isActive = true;
                if (update.getMessage().getChat().isUserChat()) {
                    threadId = 0;
                }

                startBot(chatId, threadId, userName);

                sendPoll(chatId, threadId);
            } else if (!isActive) {
                boolean isCommand = false;
                for (BotCommand command : LIST_OF_COMMANDS) {
                    if (receivedMessage.equals(command.getCommand()) || receivedMessage.equals(command.getCommand() + "@" + botName)) {
                        isCommand = true;
                        break;
                    }
                }

                if (isCommand) {
                    String sleepMessage = BotMessages.SLEEP_MESSAGE;
                    sendMessageToChat(chatId, threadId, sleepMessage, false);
                }
            }
        }
    }


    private void startBot(long chatId, int threadId, String userName) {
        String startMessage = String.format(BotMessages.START_MESSAGE, userName);

        sendMessageToChat(chatId, threadId, startMessage, false);
        log.info("Бот запущен. Ответ отправлен");
    }

    private void handleTextMessage(Message message) {
        if (message == null || message.getText() == null) {
            log.error("Получено пустое сообщение или текст сообщения равен null.");
            return;
        }

        long chatId = message.getChatId();
        Integer threadId = message.getMessageThreadId();
        if (threadId == null) {
            threadId = 0;
        }
        String receivedMessage = message.getText();
        receivedMessage = receivedMessage.toLowerCase();

        processCommandFromMessage(message, chatId, threadId, receivedMessage);
    }

    private void processCommandFromMessage(Message message, long chatId, Integer threadId, String receivedMessage) {
        String botName = getBotUsername().toLowerCase();
        if (isActive && receivedMessage.equals("/start") || receivedMessage.equals("/start@" + botName)) {
            sendMessageToChat(chatId, threadId, BotMessages.IN_USE_TEXT, false);
        }
        for (BotCommand command : LIST_OF_COMMANDS) {
            if (receivedMessage.equals(command.getCommand()) || receivedMessage.equals(command.getCommand() + "@" + botName)) {
                handleCommand(command.getCommand(), chatId, threadId, message.getFrom().getFirstName());
                return;
            }
        }
    }


    private void botAnswerUtils(String receivedMessage, long chatId, int threadId, Message message) {
        if (message == null) {
            log.error("Передано пустое сообщение.");
            return;
        }

        processCommandFromMessage(message, chatId, threadId, receivedMessage);
    }

    private void handleCommand(String command, long chatId, int threadId, String userName) {

        switch (command) {
            case "/restart":
                currentQuestionIndex = 0;
                sendPoll(chatId, threadId);
                log.info("Отправлена команда перезапуска вопросов пользователю: " + userName);
                break;
            case "/help":
                sendHelpText(chatId, threadId);
                log.info("Отправлена справка пользователю: " + userName);
                break;
            case "/exit":
                isActive = false;
                sendExitMessage(chatId, threadId);
                log.info("Пользователь " + userName + " вышел из режима бота");
                break;
            case "/next":
                sendCorrectAnswerAndNextQuestion(chatId, threadId);
                log.info("Пользователь " + userName + " ввел команду '/next'");
                break;
            default:
                break;
        }
    }


    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        if (callbackQuery == null || callbackQuery.getMessage() == null) {
            log.warn("CallbackQuery или Message равен null");
            return;
        }

        long chatId = callbackQuery.getMessage().getChatId();
        Integer threadIdInteger = null;

        // Попробуем получить threadId, если доступен
        Message message = null;
        if (callbackQuery.getMessage() instanceof Message) {
            message = (Message) callbackQuery.getMessage();
            threadIdInteger = message.getMessageThreadId();
        }

        int threadId = (threadIdInteger != null) ? threadIdInteger : 0; // Проверка на null и преобразование в int

        String receivedMessage = callbackQuery.getData();
        String userName = callbackQuery.getFrom().getFirstName();

        String replyingUserName = null; // Добавлено для хранения имени пользователя, на которого отвечает сообщение
        MaybeInaccessibleMessage replyToMessage = callbackQuery.getMessage();

        // Проверяем, является ли сообщение экземпляром Message и доступно ли оно
        if (message != null && message.getDate() != null) {
            replyingUserName = message.getFrom().getFirstName();
        } else if (replyToMessage instanceof InaccessibleMessage) {
            log.warn("Недоступное сообщение: {}", callbackQuery.getMessage().getMessageId());
            // Обработка недоступного сообщения, если необходимо
            // В данном случае мы просто пропускаем передачу сообщения в метод botAnswerUtils()
            log.info("Обработка callback query от пользователя: {} (отвечает на недоступное сообщение)", userName);
            log.info("Обработка callback query от пользователя {} завершена", userName);
            return;
        }

        log.info("Обработка callback query от пользователя: {} (отвечает на пользователя: {})", userName, replyingUserName);

        if (message != null) {
            log.info("Передача сообщения на обработку: {}", message.getMessageId());
            botAnswerUtils(receivedMessage, chatId, threadId, message);
        }

        log.info("Обработка callback query от пользователя {} завершена", userName);
    }


    private void sendPoll(long chatId, int threadId) {
        PollData[] polls = pollReader.readPolls();

        if (polls != null && polls.length > currentQuestionIndex) {
            PollData pollData = polls[currentQuestionIndex];
            SendPoll poll = getSendPoll(chatId, threadId, pollData);

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
    // Обработка inline keyboard

    private SendPoll getSendPoll(long chatId, int threadId, PollData pollData) {
        SendPoll poll = new SendPoll();
        poll.setChatId(chatId);
        poll.setMessageThreadId(threadId);
        poll.setQuestion(pollData.getQuestion());
        poll.setOptions(Arrays.asList(pollData.getOptions()));
        poll.setReplyMarkup(keyboardMarkup); // Используем поле keyboardMarkup в качестве клавиатуры
        return poll;
    }

    private void sendCorrectAnswerAndNextQuestion(long chatId, int threadId) {
        PollData[] polls = pollReader.readPolls();
        if (polls != null && currentQuestionIndex < polls.length) {
            SendMessage correctAnswerMessage = getSendMessage(chatId, threadId, polls);

            try {
                execute(correctAnswerMessage);

                if (currentQuestionIndex < polls.length - 1) {
                    currentQuestionIndex++;
                    sendPoll(chatId, threadId); // Отправляем следующий вопрос
                } else {
                    // Достигнут последний вопрос, отправляем сообщение с правильным ответом и деактивируем бота
                    getLastQuestionMessage(chatId, threadId);
                }
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    private String escapeSpecialCharacters(String input) {
        String[] specialCharacters = {"|", ".", "~", "(", ")", "'", ";", ",", "#", "@", "&", "%", "-", "$", "&"}; // Добавьте другие символы при необходимости


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

    private void getLastQuestionMessage(long chatId, int threadId) {
        String exitMessageText = BotMessages.LAST_QUESTION_TEXT;
        sendMessageToChat(chatId, threadId, exitMessageText, true);
        stopActive();
    }

    private void sendHelpText(long chatId, int threadId) {
        sendMessageToChat(chatId, threadId, BotMessages.HELP_TEXT, false);
        log.info("Отправлен ответ"); // Логируем успешную отправку
    }

    private void sendExitMessage(long chatId, int threadId) {
        String exitMessageText = BotMessages.EXIT_TEXT;
        sendMessageToChat(chatId, threadId, exitMessageText, true);
        stopActive();
    }

    private void sendMessageToChat(long chatId, int threadId, String text, boolean removeKeyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setMessageThreadId(threadId);

        message.setText(text);

        if (removeKeyboard) {
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
            keyboardRemove.setRemoveKeyboard(true);
            message.setReplyMarkup(keyboardRemove);
        }
        try {
            execute(message); // Отправляем сообщение
        } catch (TelegramApiException e) {
            log.error(e.getMessage()); // В случае ошибки выводим сообщение об ошибке в лог
        }
    }

    private void stopActive() {
        isActive = false; // Деактивируем бота после отправки сообщения
        currentQuestionIndex = 0; // Сбрасываем индекс вопроса, когда вопросы закончились
        log.info("Пользователь вышел из режима бота");
    }
}