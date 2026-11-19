package com.crm.common.infrastructure.persistence.repositories;

import com.crm.common.application.exceptions.ConcurrentUpdateException;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.repository.Criteria;
import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.infrastructure.persistence.entities.JpaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("RepositoryImpl")
class RepositoryImplTest {

    private static final class Item extends AggregateRoot {
        Item(UUID id, Long version) {
            super(id, version);
        }
    }

    static final class ItemEntity extends JpaEntity<UUID> {
    }

    interface ItemJpaRepository extends JpaRepository<ItemEntity, UUID> {
    }

    record NoCriteria() implements Criteria {
    }

    private static final class ItemMapper implements EntityMapper<Item, ItemEntity> {

        @Override
        public Item toDomain(ItemEntity persistence) {
            return new Item(persistence.getId(), persistence.getVersion());
        }

        @Override
        public ItemEntity toPersistence(Item domain) {
            var entity = new ItemEntity();

            entity.setId(domain.getId());
            entity.setVersion(domain.getVersion().orElse(null));

            return entity;
        }
    }

    private static final class ItemRepository extends RepositoryImpl<Item, NoCriteria, ItemEntity, ItemJpaRepository> {
        ItemRepository(ItemJpaRepository jpaRepository) {
            super(jpaRepository, mock(EntityManager.class), new ItemMapper(), ItemEntity.class);
        }
    }

    private final ItemJpaRepository jpaRepository = mock(ItemJpaRepository.class);
    private final ItemRepository repository = new ItemRepository(jpaRepository);

    private static ItemEntity stored(UUID id, long version) {
        var entity = new ItemEntity();

        entity.setId(id);
        entity.setVersion(version);

        return entity;
    }

    @Test
    @DisplayName("o save grava na hora e devolve o aggregate com a versão nova")
    void saveFlushesAndReturnsNewVersion() {
        var id = UUID.randomUUID();
        when(jpaRepository.saveAndFlush(any())).thenReturn(stored(id, 4L));

        var saved = repository.save(new Item(id, 3L));

        assertThat(saved.getVersion()).isEqualTo(Optional.of(4L));
        verify(jpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("versão velha no save vira concurrent_update")
    void staleVersionIsConcurrentUpdate() {
        when(jpaRepository.saveAndFlush(any())).thenThrow(new ObjectOptimisticLockingFailureException(ItemEntity.class, UUID.randomUUID()));

        assertThatThrownBy(() -> repository.save(new Item(UUID.randomUUID(), 3L)))
                .isInstanceOf(ConcurrentUpdateException.class);
    }

    @Test
    @DisplayName("violação de constraint continua conflito genérico, sem o código de escrita concorrente")
    void constraintViolationIsConflict() {
        when(jpaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicada"));

        assertThatThrownBy(() -> repository.save(new Item(UUID.randomUUID(), null)))
                .isExactlyInstanceOf(ConflictException.class)
                .hasMessage("operação conflita com um registro já existente");
    }

    @Test
    @DisplayName("o saveBatch grava o lote na hora e devolve as versões novas")
    void saveBatchFlushes() {
        var id = UUID.randomUUID();
        when(jpaRepository.saveAllAndFlush(anyList())).thenReturn(List.of(stored(id, 1L)));

        var saved = repository.saveBatch(List.of(new Item(id, 0L)));

        assertThat(saved).extracting(item -> item.getVersion().orElseThrow()).containsExactly(1L);
        verify(jpaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("versão velha no saveBatch também vira concurrent_update")
    void saveBatchStaleVersion() {
        when(jpaRepository.saveAllAndFlush(anyList())).thenThrow(new ObjectOptimisticLockingFailureException(ItemEntity.class, UUID.randomUUID()));

        assertThatThrownBy(() -> repository.saveBatch(List.of(new Item(UUID.randomUUID(), 0L))))
                .isInstanceOf(ConcurrentUpdateException.class);
    }
}
