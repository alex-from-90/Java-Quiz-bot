package com.quiz.bot.components;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class Buttons {
    private static final InlineKeyboardButton START_BUTTON = new InlineKeyboardButton("Старт");
    private static final InlineKeyboardButton HELP_BUTTON = new InlineKeyboardButton("Помощь");
    private static final InlineKeyboardButton EXIT_BUTTON = new InlineKeyboardButton("Выход");
    private static final InlineKeyboardButton NEXT_BUTTON = new InlineKeyboardButton("Следующий вопрос");

    public static InlineKeyboardMarkup inlineMarkup() {
        START_BUTTON.setCallbackData("/start");
        NEXT_BUTTON.setCallbackData("/next");
        HELP_BUTTON.setCallbackData("/help");
        EXIT_BUTTON.setCallbackData("/exit");


        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(START_BUTTON);
        rowInline.add(NEXT_BUTTON);
        rowInline.add(HELP_BUTTON);
        rowInline.add(EXIT_BUTTON);


        List<List<InlineKeyboardButton>> rowsInLine = List.of(rowInline);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        markupInline.setKeyboard(rowsInLine);

        return markupInline;
    }
}
