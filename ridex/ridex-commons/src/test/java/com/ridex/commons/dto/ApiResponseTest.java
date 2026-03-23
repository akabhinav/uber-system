package com.ridex.commons.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_setsSuccessTrue() {
        ApiResponse<String> response = ApiResponse.ok("hello");
        assertThat(response.success()).isTrue();
    }

    @Test
    void ok_setsData() {
        ApiResponse<String> response = ApiResponse.ok("test-data");
        assertThat(response.data()).isEqualTo("test-data");
    }

    @Test
    void ok_errorFieldsAreNull() {
        ApiResponse<String> response = ApiResponse.ok("data");
        assertThat(response.error()).isNull();
        assertThat(response.errorCode()).isNull();
    }

    @Test
    void ok_timestampIsSet() {
        ApiResponse<String> response = ApiResponse.ok("data");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void ok_worksWithComplexType() {
        record Person(String name, int age) {}
        Person person = new Person("Alice", 30);

        ApiResponse<Person> response = ApiResponse.ok(person);
        assertThat(response.success()).isTrue();
        assertThat(response.data().name()).isEqualTo("Alice");
        assertThat(response.data().age()).isEqualTo(30);
    }

    @Test
    void error_setsSuccessFalse() {
        ApiResponse<Object> response = ApiResponse.error("ERR_001", "Something broke");
        assertThat(response.success()).isFalse();
    }

    @Test
    void error_setsErrorCodeAndMessage() {
        ApiResponse<Object> response = ApiResponse.error("NOT_FOUND", "Resource missing");
        assertThat(response.errorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.error()).isEqualTo("Resource missing");
    }

    @Test
    void error_dataIsNull() {
        ApiResponse<Object> response = ApiResponse.error("ERR", "msg");
        assertThat(response.data()).isNull();
    }

    @Test
    void error_timestampIsSet() {
        ApiResponse<Object> response = ApiResponse.error("ERR", "msg");
        assertThat(response.timestamp()).isNotNull();
    }
}
