package com.quiz.bot.poll;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PollReader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(PollReader.class.getName());
    private static final String POLL_FILE_PATH = "polls.json";

    public PollData[] readPolls() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(POLL_FILE_PATH)) {
            if (inputStream == null) {
                throw new RuntimeException("Фаил с квизом повреждён или отсутствует.");
            }
            return objectMapper.readValue(inputStream, PollData[].class);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Произошла ошибка при чтении из файла JSON.", e);
            return null;
        }
    }
}