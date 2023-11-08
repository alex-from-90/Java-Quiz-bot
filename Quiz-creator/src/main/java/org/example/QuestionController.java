package org.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class QuestionController {
    private List<Question> questions = new ArrayList<>();
    @GetMapping("/")
    public String home() {
        // Здесь вы можете добавить логику, которая будет выполняться при запросе корневого пути
        return "home"; // Вернуть имя представления (например, "home.html")
    }
    @GetMapping("/questions")
    public String getQuestions(Model model) {
        model.addAttribute("questions", questions);
        return "questions";
    }

    @PostMapping("/questions")
    public String addQuestion(Question question) {
        questions.add(question);
        return "redirect:/questions";
    }

    @GetMapping("/generateJson")
    public String generateJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Преобразуйте список вопросов в JSON
            String json = objectMapper.writeValueAsString(questions);

            // Задайте путь к файлу, в который вы хотите записать JSON
            String filePath = "questions.json";

            // Запишите JSON в файл
            objectMapper.writeValue(new File(filePath), questions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/questions";
    }
}
