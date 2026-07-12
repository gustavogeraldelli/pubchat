package dev.pubchat.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomRepositoryTest {

    @Test
    void shouldStoreRoomOnlyOnce() {
        RoomRepository repository = new RoomRepository();

        repository.add("room-1");
        repository.add("room-1");

        assertThat(repository.getAll()).containsExactly("room-1");
    }

    @Test
    void shouldReturnWhetherRoomExists() {
        RoomRepository repository = new RoomRepository();

        repository.add("room-1");

        assertThat(repository.exists("room-1")).isTrue();
        assertThat(repository.exists("room-2")).isFalse();
    }

    @Test
    void shouldReturnImmutableRoomSnapshot() {
        RoomRepository repository = new RoomRepository();

        repository.add("room-1");

        assertThatThrownBy(() -> repository.getAll().add("room-2"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(repository.getAll()).containsExactly("room-1");
    }
}
