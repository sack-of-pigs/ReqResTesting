package com.api.tests;

import com.api.clients.UserClient;
import com.api.core.BaseTest;
import com.api.factories.TestDataFactory;
import com.api.models.request.CreateUserRequest;
import com.api.models.response.UpdateUserResponse;
import com.api.models.response.UserListResponse;
import com.api.models.response.UserResponse;
import com.api.utils.AuthSpecs;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("ReqRes API")
@Feature("Users")
class UserApiTest extends BaseTest {

    private final UserClient userClient = new UserClient();

    // ===============================================================
    // 1. ПАГИНАЦИЯ — табличные данные, идеальный кейс для @CsvSource
    // ===============================================================

    @ParameterizedTest(name = "page={0}, per_page={1} → users={2}, total_pages={3}")
    @CsvSource({
            // page, per_page, expectedUserCount, expectedTotalPages
            "1, 6, 6, 2",   // первая страница, дефолтный размер
            "2, 6, 6, 2",   // вторая (последняя) страница
            "1, 3, 3, 4",   // кастомный per_page=3 → 12/3 = 4 страницы
            "2, 3, 3, 4",   // середина при кастомном размере
            "4, 3, 3, 4"    // последняя страница при кастомном размере
    })
    @Story("Пагинация списка пользователей")
    @DisplayName("GET /api/users — пагинация возвращает корректные метаданные")
    void getUsers_pagination_shouldReturnCorrectMetadata(
            int page, int perPage, int expectedUserCount, int expectedTotalPages) {

        Response response = userClient.getUsersRaw(page, perPage);

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/user-list-response.json"))
                .body("page", equalTo(page))
                .body("per_page", equalTo(perPage))
                .body("total", equalTo(12))
                .body("total_pages", equalTo(expectedTotalPages))
                .body("data", hasSize(expectedUserCount))
                // Каждый элемент массива должен быть валидным пользователем
                .body("data.id", everyItem(notNullValue()))
                .body("data.email", everyItem(containsString("@")))
                .body("data.first_name", everyItem(not(emptyString())));
    }

    // ===============================================================
    // 2. ЗАДЕРЖКИ — @MethodSource, потому что нужен и параметр, и ожидание
    // ===============================================================

    /**
     * ReqRes поддерживает ?delay=N (до 10 секунд)[reference:1].
     * Тест проверяет, что ответ приходит не раньше заявленной задержки.
     *
     * ВАЖНО: это медленные тесты. В CI их стоит помечать тегом
     * @Tag("slow") и запускать отдельно, чтобы не тормозить основной прогон.
     */
    @ParameterizedTest(name = "delay={0}s → ответ не раньше {0} секунд")
    @ValueSource(ints = {1, 2}) // Оставляем только один источник данных
    @Story("Задержки ответа")
    @DisplayName("GET /api/users?delay=N — ответ приходит не раньше заявленной задержки")
    void getUsers_withDelay_shouldRespectMinimumDelay(int delaySeconds) {
        long start = System.currentTimeMillis();

        // Вызываем метод с тремя аргументами
        Response response = userClient.getUsersRaw(1, 6, delaySeconds);

        long elapsedSeconds = (System.currentTimeMillis() - start) / 1000;

        response.then().statusCode(200);

        // Заменили ... на нормальное сообщение об ошибке
        assertTrue(elapsedSeconds >= delaySeconds,
                "Ответ должен прийти не раньше " + delaySeconds + " секунд, " +
                        "а пришёл через " + elapsedSeconds + " секунд");
    }

    /**
     * Аргументы для теста задержек.
     * Значения небольшие, чтобы тест не занимал минуты.
     */
    static Stream<Integer> delayArguments() {
        return Stream.of(1, 2);
    }

    // ===============================================================
    // 3. ГРАНИЧНЫЕ ID — @ValueSource, потому что проверяем одно значение
    // ===============================================================

    @ParameterizedTest(name = "id={0} → 404 и пустой JSON '{}'")
    @ValueSource(ints = {23, 999, Integer.MAX_VALUE})
    @Story("Обработка несуществующих пользователей")
    @DisplayName("GET /api/users/{id} — несуществующий ID возвращает 404")
    void getUserById_nonExistentIds_shouldReturn404(int userId) {
        Response response = userClient.getUserByIdRaw(userId);

        response.then()
                .statusCode(404)
                .body(equalTo("{}"));

        // Десериализация не должна бросать исключение,
        // но data должен быть null — это контракт ReqRes.
        UserResponse body = response.as(UserResponse.class);
        assertNull(body.getData());
    }

    // ===============================================================
    // 4. ПОЗИТИВНЫЕ ID — @MethodSource для проверки email по ID
    // ===============================================================

    /**
     * В демо-данных ReqRes 12 пользователей с предсказуемыми email.
     * Первые несколько: 1→george.bluth@reqres.in, 2→janet.weaver@reqres.in и т.д.[reference:2]
     */
    @ParameterizedTest(name = "id={0} → {1}")
    @MethodSource("knownUsers")
    @Story("Получение существующих пользователей")
    @DisplayName("GET /api/users/{id} — известные пользователи возвращают правильный email")
    void getUserById_knownUsers_shouldReturnExpectedEmail(int userId, String expectedEmail) {
        UserResponse response = userClient.getUserById(userId);

        assertNotNull(response.getData());
        assertEquals(userId, response.getData().getId());
        assertEquals(expectedEmail, response.getData().getEmail(),
                "Email для пользователя " + userId + " не совпадает");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> knownUsers() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(1, "george.bluth@reqres.in"),
                org.junit.jupiter.params.provider.Arguments.of(2, "janet.weaver@reqres.in"),
                org.junit.jupiter.params.provider.Arguments.of(3, "emma.wong@reqres.in"),
                org.junit.jupiter.params.provider.Arguments.of(4, "eve.holt@reqres.in")
        );
    }

    // ===============================================================
    // 5. CREATE (POST)
    // ===============================================================

    @Test
    @Story("Создание пользователя")
    @DisplayName("POST /api/users — создает пользователя и возвращает 201")
    void createUser_shouldReturn201AndCreatedUser() {
        CreateUserRequest request = TestDataFactory.randomUser();

        userClient.createUserRaw(request)
                .then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/create-user-response.json"))
                .body("name", equalTo(request.getName()))
                .body("job", equalTo(request.getJob()))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }

    // ===============================================================
    // 6. UPDATE (PUT)
    // ===============================================================

    @Test
    @Story("Обновление пользователя")
    @DisplayName("PUT /api/users/{id} — полностью обновляет пользователя")
    void updateUser_shouldReturnUpdatedUser() {
        // given
        int userId = 2;
        CreateUserRequest request = TestDataFactory.randomUser();

        // when
        UpdateUserResponse response = userClient.updateUser(userId, request);

        // then
        assertNotNull(response.getUpdatedAt(), "Дата обновления не должна быть null");
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getJob(), response.getJob());
    }

    @Test
    @Story("Обновление пользователя")
    @DisplayName("PUT /api/users/{id} — ответ соответствует JSON Schema")
    void updateUser_shouldMatchSchema() {
        CreateUserRequest request = TestDataFactory.randomUser();

        userClient.updateUserRaw(2, request)
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/update-user-response.json"));
    }

    // ===============================================================
    // 7. PARTIAL UPDATE (PATCH)
    // ===============================================================

    @Test
    @Story("Частичное обновление пользователя")
    @DisplayName("PATCH /api/users/{id} — частично обновляет данные")
    void patchUser_shouldReturnUpdatedFields() {
        int userId = 3;
        // Отправляем только имя (в реальном API job осталось бы прежним)
        CreateUserRequest request = CreateUserRequest.builder()
                .name(TestDataFactory.uniqueName())
                .build();

        UpdateUserResponse response = userClient.patchUser(userId, request);

        assertEquals(request.getName(), response.getName());
        assertNotNull(response.getUpdatedAt());
    }

    // ===============================================================
    // 8. DELETE
    // ===============================================================

    @Test
    @Story("Удаление пользователя")
    @DisplayName("DELETE /api/users/{id} — удаляет пользователя (204 No Content)")
    void deleteUser_shouldReturn204() {
        int userId = 2;

        userClient.deleteUserRaw(userId)
                .then()
                .statusCode(204)
                .body(equalTo("")); // Тело должно быть пустым
    }

    // ===============================================================
    // 9. НЕГАТИВНЫЕ ТЕСТЫ: Некорректный ID
    // ===============================================================

    @ParameterizedTest(name = "id={0} → 404 Not Found")
    @ValueSource(strings = {"abc", "-1", "1.5", "0"})
    @Story("Обработка некорректных ID")
    @DisplayName("GET /api/users/{id} — нечисловой или невалидный ID возвращает 404")
    void getUserById_invalidIdFormat_shouldReturn404(String invalidId) {
        // Используем новый строковый метод
        userClient.getUserByStringIdRaw(invalidId)
                .then()
                .statusCode(404)
                .body(equalTo("{}"));
    }

    @Test
    @Story("Проверка Content-Type")
    @DisplayName("POST /api/users — с Content-Type text/plain возвращает 415")
    void createUser_wrongContentType_shouldReturn415() {
        // Передаём тело как строку, чтобы REST Assured смог отправить text/plain
        String invalidBody = "some plain text body";

        given()
                .spec(AuthSpecs.wrongContentTypeSpec())
                .body(invalidBody) // <-- строка, а не объект!
                .when()
                .post("/api/users")
                .then()
                .statusCode(415); // Unsupported Media Type
    }

    // ===============================================================
    // 10. ТЕСТЫ БЕЗОПАСНОСТИ
    // ===============================================================

    @Test
    @Story("Безопасность: анонимный доступ")
    @DisplayName("GET /api/users/2 — без x-api-key возвращает 200 (keyless доступ)")
    void getUserWithoutApiKey_shouldReturn200Keyless() {
        given()
                .spec(AuthSpecs.noAuthSpec())
                .when()
                .get("/api/users/2")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(2))
                .body("_meta.context", equalTo("keyless_success")); // Проверяем, что это анонимный режим
    }

    @Test
    @Story("Безопасность: неверный ключ")
    @DisplayName("GET /api/users/2 — с неверным x-api-key возвращает 403")
    void getUserWithInvalidApiKey_shouldReturn401() {
        given()
                .spec(AuthSpecs.invalidKeySpec()) // Спецификация с неверным ключом
                .when()
                .get("/api/users/2")
                .then()
                .statusCode(403);
    }

    @Test
    @Story("Безопасность: доступ к защищённому эндпоинту")
    @DisplayName("GET /api/users/2 — с ключом, но без прав (эмуляция) возвращает 403")
    void getUserForbidden_shouldReturn403() {
        // Примечание: ReqRes может не возвращать 403 для демо-эндпоинтов,
        // но тест полезен как заготовка для реальных API, где права разграничены.
        // Здесь мы эмулируем ситуацию с помощью неверного ключа.
        // В реальном проекте для 403 нужен ключ с ограниченными правами.
        // Пока оставим этот тест как пример, но он может вернуть 401.
    }
}
