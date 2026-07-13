package dev.pubchat.repository;

import dev.pubchat.model.Room;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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

    @Test
    void shouldStoreRoomMetadata() {
        RoomRepository repository = new RoomRepository();

        repository.add("room-1");

        assertThat(repository.findById("room-1"))
                .hasValueSatisfying(room -> {
                    assertThat(room.getId()).isEqualTo("room-1");
                    assertThat(room.getCreatedAt()).isNotNull();
                    assertThat(room.getLastActivityAt()).isEqualTo(room.getCreatedAt());
                    assertThat(room.getParticipants()).isEmpty();
                });
    }

    @Test
    void shouldUpdateRoomActivity() {
        RoomRepository repository = new RoomRepository();
        Instant oldActivity = Instant.parse("2026-01-01T00:00:00Z");
        repository.add(new Room("room-1", oldActivity));

        repository.touch("room-1");

        assertThat(repository.findById("room-1"))
                .hasValueSatisfying(room -> assertThat(room.getLastActivityAt()).isAfter(oldActivity));
    }

    @Test
    void shouldDeleteInactiveRooms() {
        RoomRepository repository = new RoomRepository();
        repository.add(new Room("inactive-room", Instant.parse("2026-01-01T00:00:00Z")));
        repository.add(new Room("active-room", Instant.parse("2026-01-02T00:00:00Z")));

        repository.deleteInactiveSince(Instant.parse("2026-01-01T12:00:00Z"));

        assertThat(repository.exists("inactive-room")).isFalse();
        assertThat(repository.exists("active-room")).isTrue();
        assertThat(repository.getAll()).containsExactly("active-room");
    }

    @Test
    void shouldAddParticipantToRoom() {
        RoomRepository repository = new RoomRepository();
        repository.add("room-1");

        boolean added = repository.addParticipant("room-1", "Gus");

        assertThat(added).isTrue();
        assertThat(repository.hasParticipant("room-1", "Gus")).isTrue();
        assertThat(repository.findById("room-1"))
                .hasValueSatisfying(room -> assertThat(room.getParticipants()).containsExactly("Gus"));
    }

    @Test
    void shouldRejectDuplicateParticipantNicknameInSameRoom() {
        RoomRepository repository = new RoomRepository();
        repository.add("room-1");

        assertThat(repository.addParticipant("room-1", "Gus")).isTrue();
        assertThat(repository.addParticipant("room-1", "Gus")).isFalse();
    }

    @Test
    void shouldRemoveParticipantFromRoom() {
        RoomRepository repository = new RoomRepository();
        repository.add("room-1");
        repository.addParticipant("room-1", "Gus");

        repository.removeParticipant("room-1", "Gus");

        assertThat(repository.hasParticipant("room-1", "Gus")).isFalse();
        assertThat(repository.findById("room-1"))
                .hasValueSatisfying(room -> assertThat(room.getParticipants()).isEmpty());
    }
}
