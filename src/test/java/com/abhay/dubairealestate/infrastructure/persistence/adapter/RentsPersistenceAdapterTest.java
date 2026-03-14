package com.abhay.dubairealestate.infrastructure.persistence.adapter;

import com.abhay.dubairealestate.application.port.in.RentsFilter;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import com.abhay.dubairealestate.infrastructure.persistence.entity.RentTransactionEntity;
import com.abhay.dubairealestate.infrastructure.persistence.projection.AreaAveragePriceProjection;
import com.abhay.dubairealestate.infrastructure.persistence.repository.RentTransactionJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RentsPersistenceAdapter}.
 * Repository and JdbcTemplate are mocked to focus on mapping and delegation logic.
 */
@ExtendWith(MockitoExtension.class)
class RentsPersistenceAdapterTest {

    @Mock private RentTransactionJpaRepository repository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private RentsPersistenceAdapter adapter;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private RentTransactionEntity entity() {
        RentTransactionEntity e = new RentTransactionEntity();
        e.setId(1L);
        e.setContractDate(LocalDate.of(2025, 1, 15));
        e.setAnnualAmount(new BigDecimal("60000"));
        e.setStartDate(LocalDate.of(2025, 2, 1));
        e.setEndDate(LocalDate.of(2026, 1, 31));
        e.setAreaName("Downtown Dubai");
        e.setProjectName("Burj Khalifa");
        e.setUsage("Residential");
        e.setPropertyType("Apartment");
        e.setRooms("1 B/R");
        e.setActualArea(new BigDecimal("75.50"));
        return e;
    }

    private RentTransaction domain() {
        return new RentTransaction(null, LocalDate.of(2025, 1, 15),
                new BigDecimal("60000"), LocalDate.of(2025, 2, 1), LocalDate.of(2026, 1, 31),
                "Downtown Dubai", "Burj Khalifa", "Residential", "Apartment", "1 B/R",
                new BigDecimal("75.50"));
    }

    private AreaAveragePriceProjection projection(String name, Double avg, Long count) {
        AreaAveragePriceProjection p = mock(AreaAveragePriceProjection.class);
        when(p.getAreaName()).thenReturn(name);
        when(p.getAveragePrice()).thenReturn(avg);
        when(p.getTransactionCount()).thenReturn(count);
        return p;
    }

    // ---------------------------------------------------------------
    // saveRentsTransactions
    // ---------------------------------------------------------------

    @Test
    void saveRentsTransactions_callsBatchUpdateOnJdbcTemplate() {
        adapter.saveRentsTransactions(List.of(domain()));

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void saveRentsTransactions_batchRowContainsCorrectValues() {
        adapter.saveRentsTransactions(List.of(domain()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());

        Object[] row = captor.getValue().get(0);
        assertThat(row[4]).isEqualTo("Downtown Dubai"); // area_name
        assertThat(row[6]).isEqualTo("Residential");    // usage
    }

    @Test
    void saveRentsTransactions_nullDatesAreConvertedToNullInBatch() {
        RentTransaction noDate = new RentTransaction(null, null, null,
                null, null, "Area", null, null, null, null, null);

        adapter.saveRentsTransactions(List.of(noDate));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());

        Object[] row = captor.getValue().get(0);
        assertThat(row[0]).isNull(); // contract_date
        assertThat(row[2]).isNull(); // start_date
        assertThat(row[3]).isNull(); // end_date
    }

    @Test
    void saveRentsTransactions_largeListIsBatchedInChunks() {
        // 1200 transactions should result in 3 batchUpdate calls (500, 500, 200)
        List<RentTransaction> big = java.util.Collections.nCopies(1200, domain());

        adapter.saveRentsTransactions(big);

        verify(jdbcTemplate, times(3)).batchUpdate(anyString(), anyList());
    }

    // ---------------------------------------------------------------
    // loadRents
    // ---------------------------------------------------------------

    @Test
    void loadRents_queriesRepositoryAndMapsToDomain() {
        RentsFilter filter = new RentsFilter(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(entity())));

        Page<RentTransaction> result = adapter.loadRents(filter, pageable);

        assertThat(result).hasSize(1);
        RentTransaction t = result.getContent().get(0);
        assertThat(t.id()).isEqualTo(1L);
        assertThat(t.contractDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(t.annualAmount()).isEqualByComparingTo("60000");
        assertThat(t.areaName()).isEqualTo("Downtown Dubai");
    }

    // ---------------------------------------------------------------
    // loadRentsAreas
    // ---------------------------------------------------------------

    @Test
    void loadRentsAreas_returnsDistinctAreaNamesFromRepository() {
        when(repository.findDistinctAreaNames()).thenReturn(List.of("Area A", "Area B", "Area C"));

        assertThat(adapter.loadRentsAreas()).containsExactly("Area A", "Area B", "Area C");
    }

    // ---------------------------------------------------------------
    // loadAverageRentsPrices
    // ---------------------------------------------------------------

    @Test
    void loadAverageRentsPrices_mapsProjectionsToDomainObjects() {
        AreaAveragePriceProjection proj = projection("Downtown", 75000.0, 100L);
        when(repository.findAveragePricesPerArea()).thenReturn(List.of(proj));

        List<AreaAveragePrice> result = adapter.loadAverageRentsPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).areaName()).isEqualTo("Downtown");
        assertThat(result.get(0).averagePrice()).isEqualByComparingTo("75000.00");
        assertThat(result.get(0).transactionCount()).isEqualTo(100L);
    }

    @Test
    void loadAverageRentsPrices_nullAveragePriceMapsToZero() {
        AreaAveragePriceProjection proj = projection("Area", null, 0L);
        when(repository.findAveragePricesPerArea()).thenReturn(List.of(proj));

        List<AreaAveragePrice> result = adapter.loadAverageRentsPrices();

        assertThat(result.get(0).averagePrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------
    // loadAverageRentsPriceByArea
    // ---------------------------------------------------------------

    @Test
    void loadAverageRentsPriceByArea_foundReturnsPresent() {
        AreaAveragePriceProjection proj = projection("Marina", 80000.0, 50L);
        when(repository.findAveragePriceByArea("Marina")).thenReturn(Optional.of(proj));

        Optional<AreaAveragePrice> result = adapter.loadAverageRentsPriceByArea("Marina");

        assertThat(result).isPresent();
        assertThat(result.get().areaName()).isEqualTo("Marina");
        assertThat(result.get().averagePrice()).isEqualByComparingTo("80000.00");
    }

    @Test
    void loadAverageRentsPriceByArea_notFoundReturnsEmpty() {
        when(repository.findAveragePriceByArea("Nobody")).thenReturn(Optional.empty());

        assertThat(adapter.loadAverageRentsPriceByArea("Nobody")).isEmpty();
    }

    // ---------------------------------------------------------------
    // loadLastRentsByArea
    // ---------------------------------------------------------------

    @Test
    void loadLastRentsByArea_queriesWithCorrectPageableAndMapsToDomain() {
        when(repository.findLastByAreaName(eq("Downtown"), any(Pageable.class)))
                .thenReturn(List.of(entity()));

        List<RentTransaction> result = adapter.loadLastRentsByArea("Downtown", 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).areaName()).isEqualTo("Downtown Dubai");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findLastByAreaName(eq("Downtown"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }
}
