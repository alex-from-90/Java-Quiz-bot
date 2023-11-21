package com.quiz.bot.polls;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PollReader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(PollReader.class.getName());
    private final URL pollFileURL;

    public PollReader(String botQuizUrl) {
        URL url = null;
        try {
            url = new URL(botQuizUrl);
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Ошибка формирования URL.", e);
        }
        this.pollFileURL = url;
    }

    public PollData[] readPolls() {
        if (pollFileURL == null) {
            logger.log(Level.SEVERE, "Некорректный URL.");
            return null;
        }

        try (InputStream inputStream = pollFileURL.openStream()) {
            return objectMapper.readValue(inputStream, PollData[].class);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Произошла ошибка при чтении из файла JSON.", e);
            return null;
        }
    }
}
