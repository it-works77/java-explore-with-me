package ru.practicum.stats;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(classes = StatsServiceApp.class)
class StatsServiceAppTest {

    @Test
    void contextLoads() {
    }
}
