# Используйте образ на основе Amazon Corretto 17
FROM amazoncorretto:17-alpine-jdk

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR файл в контейнер
COPY target/*.jar app.jar

# Определяем команду для запуска JAR файла
CMD ["java", "-jar", "app.jar"]