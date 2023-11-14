package com.generator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generator.model.Poll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/")
public class FormController {

    @RequestMapping("/")
    public String showForm() {
        return "form";
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitForm(@RequestParam("question") String question, @RequestParam("options") List<String> options, @RequestParam("correctAnswer") String correctAnswer) {
        // Создаем экземпляр Poll и устанавливаем в него данные из формы
        Poll poll = new Poll(question, options, correctAnswer);

        poll.setQuestion(question);
        poll.setOptions(options);
        poll.setCorrectAnswer(correctAnswer);

        // Преобразуем данные в JSON
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(poll);
            writeToJsonFile(json);
            return ResponseEntity.ok(json);
        } catch (JsonProcessingException e) {
            log.error("Ошибка при преобразовании в JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при преобразовании в JSON");
        }
    }

    private void writeToJsonFile(String json) {
        try {
            File file = new File("polls.json");
            ObjectMapper mapper = new ObjectMapper();

            List<Poll> polls;

            // Если файл существует и не пуст, читаем данные из него
            if (file.exists() && file.length() > 0) {
                polls = mapper.readValue(file, new TypeReference<List<Poll>>() {});
            } else {
                polls = new ArrayList<>();
            }

            // Добавляем новый опрос к существующим данным
            polls.add(mapper.readValue(json, Poll.class));
            mapper.writeValue(file, polls); // Записываем обновленный список в файл
            log.info("Данные успешно записаны в файл");
        } catch (IOException e) {
            log.error("Ошибка при записи в файл", e);
        }
    }
}
