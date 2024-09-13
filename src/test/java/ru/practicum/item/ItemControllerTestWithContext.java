package ru.practicum.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.practicum.config.WebConfig;
import ru.practicum.item.dto.AddItemRequest;
import ru.practicum.item.dto.ItemDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig({ItemController.class, ItemControllerTestConfig.class, WebConfig.class})
class ItemControllerTestWithContext {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ItemService itemService;

    private MockMvc mvc;

    private AddItemRequest request;

    @Autowired
    ItemControllerTestWithContext(ItemService itemService) {
        this.itemService = itemService;
    }

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        request = AddItemRequest.builder()
                .url("https://google.com")
                .tags(Set.of())
                .build();
    }

    @Test
    void add() throws Exception {
        when(itemService.addNewItem(anyLong(), any(AddItemRequest.class)))
                .thenReturn(
                        ItemDto.builder()
                                .id(1L)
                                .userId(1L)
                                .url("https://google.com")
                                .resolvedUrl("https://google.com")
                                .title("google")
                                .dateResolved(hoursFromNow(-2))
                                .hasImage(true)
                                .hasVideo(false)
                                .unread(true)
                                .build()
                );
        mvc.perform(
                        post("/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                                .header("X-Later-User-Id", 1)
                )
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.id").value(1),
                        jsonPath("$.resolvedUrl").value("https://google.com"),
                        jsonPath("$.dateResolved").value(notNullValue())
                );
    }

    private Instant hoursFromNow(int hours) {
        LocalDateTime localDateTime = LocalDateTime.now().plusHours(hours);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}