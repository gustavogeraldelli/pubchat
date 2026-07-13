package dev.pubchat.controller;

import dev.pubchat.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomControllerTest {

    private final RoomRepository repository = new RoomRepository();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new RoomController(repository))
            .build();

    @Test
    void shouldCreateRoomAndStoreIt() throws Exception {
        String roomId = mockMvc.perform(post("/api/rooms"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(roomId).isNotBlank();
        assertThat(repository.exists(roomId)).isTrue();
    }

    @Test
    void shouldReturnOkWhenRoomExists() throws Exception {
        repository.add("room-1");

        mockMvc.perform(get("/api/rooms/room-1/available"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenRoomDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/rooms/missing-room/available"))
                .andExpect(status().isNotFound());
    }
}
