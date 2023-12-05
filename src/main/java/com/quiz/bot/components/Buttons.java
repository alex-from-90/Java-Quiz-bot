package com.quiz.bot.components;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public class Buttons {
    // Определения кнопок
    private static final InlineKeyboardButton HELP_BUTTON = new InlineKeyboardButton("Помощь");
    private static final InlineKeyboardButton EXIT_BUTTON = new InlineKeyboardButton("Выход");
    private static final InlineKeyboardButton NEXT_BUTTON = new InlineKeyboardButton("Следующий вопрос");
    private static final InlineKeyboardButton RESTART_BUTTON = new InlineKeyboardButton("Перезапуск");
    public static InlineKeyboardMarkup inlineMarkup() {
        // Устанавливаем callback данные для кнопок
        NEXT_BUTTON.setCallbackData("/next");
        HELP_BUTTON.setCallbackData("/help");
        EXIT_BUTTON.setCallbackData("/exit");
        RESTART_BUTTON.setCallbackData("/restart");

        // Создаем строки с кнопками
        List<InlineKeyboardButton> row1 = List.of(HELP_BUTTON, EXIT_BUTTON, RESTART_BUTTON);
        List<InlineKeyboardButton> row2 = List.of(NEXT_BUTTON);
        // Создаем встроенную клавиатуру с двумя строками
        List<List<InlineKeyboardButton>> rowsInline = List.of(row1, row2);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        markupInline.setKeyboard(rowsInline);

        return markupInline;
    }
}

