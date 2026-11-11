package ru.practicum.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(controllers = StatsController.class)
class StatsControllerTest {

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    StatsService statsService;

    @Autowired
    MockMvc mockMvc;


}