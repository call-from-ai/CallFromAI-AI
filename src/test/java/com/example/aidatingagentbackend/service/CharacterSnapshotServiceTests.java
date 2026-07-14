package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.CharacterSnapshotEntity;
import com.example.aidatingagentbackend.repository.CharacterSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterSnapshotServiceTests {
    @Mock CharacterSnapshotRepository repository;

    @Test
    void createsNewSnapshot() {
        CharacterSnapshotService service = new CharacterSnapshotService(repository);
        service.upsert(10L, snapshot("하나", 1));
        verify(repository).save(argThat(entity -> entity.getCharacterId() == 10L && entity.getName().equals("하나")));
    }

    @Test
    void updatesExistingSnapshotAndRepeatedRequestDoesNotCreateAnotherEntity() {
        CharacterSnapshotEntity existing = new CharacterSnapshotEntity(snapshot("하나", 1));
        when(repository.findByCharacterId(10L)).thenReturn(Optional.of(existing));
        CharacterSnapshotService service = new CharacterSnapshotService(repository);

        service.upsert(10L, snapshot("하나2", 2));
        service.upsert(10L, snapshot("하나2", 2));

        assertEquals("하나2", existing.getName());
        verify(repository, times(2)).save(same(existing));
    }

    @Test
    void rejectsMismatchedCharacterId() {
        assertThrows(IllegalArgumentException.class,
                () -> new CharacterSnapshotService(repository).upsert(11L, snapshot("하나", 1)));
        verifyNoInteractions(repository);
    }

    @Test
    void lowerCalculationVersionDoesNotOverwrite() {
        CharacterSnapshotEntity existing = new CharacterSnapshotEntity(snapshot("최신", 2));
        when(repository.findByCharacterId(10L)).thenReturn(Optional.of(existing));

        new CharacterSnapshotService(repository).upsert(10L, snapshot("과거", 1));

        assertEquals("최신", existing.getName());
        verify(repository, never()).save(any());
    }

    private CharacterSnapshot snapshot(String name, int version) {
        return new CharacterSnapshot(10L, name, "mind", "style", "DEVELOPER", AgentLifeType.WORKER, 72,
                new CharacterTraitSnapshot(6, 7, 8, 9, 5, 2, 4, 7, 8, 7, version));
    }
}
