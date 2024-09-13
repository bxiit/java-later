package ru.practicum.item;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class ItemControllerTestConfig {

    @Bean
    public ItemService itemService() {
        return mock(ItemService.class);
    }
}
