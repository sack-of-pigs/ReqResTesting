package com.api.factories;

import com.github.javafaker.Faker;
import com.api.models.request.CreateUserRequest;

import java.util.Locale;
import java.util.UUID;

/**
 * Фабрика тестовых данных.
 *
 * Гарантирует, что каждый вызов возвращает УНИКАЛЬНЫЙ объект:
 *   - Faker генерирует реалистичные, но случайные значения
 *   - UUID добавляется как суффикс, чтобы исключить коллизии
 *     даже при параллельном запуске тестов в одной миллисекунде
 *
 * Класс утилитный: final, приватный конструктор.
 */
public final class TestDataFactory {

    private static final Faker FAKER = new Faker(new Locale("en"));

    private TestDataFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ---------------------------------------------------------------
    // Готовые объекты запросов
    // ---------------------------------------------------------------

    /**
     * Случайный пользователь для POST /api/users.
     * Поля name и job соответствуют контракту ReqRes.
     */
    public static CreateUserRequest randomUser() {
        return CreateUserRequest.builder()
                .name(uniqueName())
                .job(FAKER.job().title())
                .build();
    }

    /**
     * Пользователь с заданным именем — удобно, когда в тесте
     * важна читаемость («создаём пользователя по имени Alice»),
     * но нужна уникальность для изоляции.
     */
    public static CreateUserRequest userWithName(String baseName) {
        return CreateUserRequest.builder()
                .name(uniqueName(baseName))
                .job(FAKER.job().title())
                .build();
    }

    // ---------------------------------------------------------------
    // Примитивы — используются фабричными методами выше,
    // но могут пригодиться и напрямую в тестах
    // ---------------------------------------------------------------

    /** Уникальное полное имя: "Janet Weaver-7f3a1b2c". */
    public static String uniqueName() {
        return uniqueName(FAKER.name().fullName());
    }

    /** Уникальное имя на основе базового: "Alice-7f3a1b2c". */
    public static String uniqueName(String baseName) {
        return baseName + "-" + shortUuid();
    }

    /** Уникальный email: "janet.weaver.7f3a1b2c@example.com". */
    public static String uniqueEmail() {
        String local = FAKER.name().username().replaceAll("[^a-zA-Z0-9]", "");
        return local + "." + shortUuid() + "@example.com";
    }

    /** Уникальный job title: "Lead Solutions Architect-7f3a1b2c". */
    public static String uniqueJob() {
        return FAKER.job().title() + "-" + shortUuid();
    }

    // ---------------------------------------------------------------
    // Внутреннее
    // ---------------------------------------------------------------

    /**
     * Короткий UUID (8 символов) — достаточно для исключения
     * коллизий в тестовом прогоне и при этом не раздувает логи.
     */
    private static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
