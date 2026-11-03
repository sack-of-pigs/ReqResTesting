package com.api.core;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Базовый класс для всех API-тестов.
 *
 * Задачи:
 *   1. Один раз на JVM включает логирование запроса/ответа
 *      при падении валидации (а не всегда — иначе логи нечитаемы).
 *   2. Настраивает глобальные параметры REST Assured,
 *      которые не относятся к конкретному запросу
 *      (например, отключение лишних заголовков).
 *   3. Даёт наследникам общий логгер с именем класса.
 *
 * НЕ содержит ассертов и не создаёт клиентов —
 * это ответственность конкретных тестов.
 */
public abstract class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Выполняется один раз перед всеми тестами в JVM.
     * Статический метод — JUnit вызывает его единожды
     * на весь прогон, а не на каждый тестовый класс.
     */
    @BeforeAll
    static void configureRestAssured() {
        // Логировать запрос и ответ ТОЛЬКО если валидация упала.
        // Это главный инструмент отладки: в зелёном прогоне —
        // тишина, в красном — полный запрос, ответ и заголовки.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Отключаем автоматический заголовок "Accept-Encoding: gzip,deflate".
        // Некоторые сервера (и прокси) с ним ведут себя непредсказуемо,
        // а для API-тестов сжатие ответа нам не нужно.
        RestAssured.urlEncodingEnabled = true;
    }

    /**
     * Хук, который наследники могут переопределить,
     * чтобы видеть в логах имя текущего теста.
     *
     * Переопределять НЕ обязательно — просто удобно,
     * когда в логе несколько тестов подряд.
     */
    protected void logTestName(TestInfo testInfo) {
        log.info("▶ {}", testInfo.getDisplayName());
    }
}
