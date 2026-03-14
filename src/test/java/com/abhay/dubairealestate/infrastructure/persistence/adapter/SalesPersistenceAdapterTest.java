package com.abhay.dubairealestate.infrastructure.persistence.adapter;

import com.abhay.dubairealestate.application.port.in.SalesFilter;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import com.abhay.dubairealestate.infrastructure.persistence.entity.SaleTransactionEntity;
import com.abhay.dubairealestate.infrastructure.persistence.projection.AreaAveragePriceProjection;
import com.abhay.dubairealestate.infrastructure.persistence.repository.SaleTransactionJpaRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesPersistenceAdapter}.
 * Repository and JdbcTemplate are mocked to focus on mapping and delegation logic.
 */
@ExtendWith(MockitoExtension.class)
class SalesPersistenceAdapterTest {

    @Mock private SaleTransactionJpaRepository repository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private SalesPersistenceAdapter adapter;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private SaleTransactionEntity entity() {
        SaleTransactionEntity e = new SaleTransactionEntity();
        e.setId(1L);
        e.setTransId("T001");
        e.setTransDate(LocalDate.of(2025, 3, 10));
        e.setTransValue(new BigDecimal("2500000"));
        e.setAreaName("Downtown Dubai");
        e.setProjectName("Emaar Tower");
        e.setUsage("Residential");
        e.setRegistrationType("Sales");
        e.setPropertyType("Apartment");
        e.setRooms("2 B/R");
        e.setActualArea(new BigDecimal("120.50"));
        e.setMeterSalePrice(new BigDecimal("20746.89"));
        return e;
    }

    private SaleTransaction domain() {
        return new SaleTransaction(null, "T001", LocalDate.of(2025, 3, 10),
                new BigDecimal("2500000"), "Downtown Dubai", "Emaar Tower", "Residential",
                "Sales", "Apartment", "2 B/R", new BigDecimal("120.50"),
                new BigDecimal("20746.89"));
    }

    private AreaAveragePriceProjection projection(String name, Double avg, Long count) {
        AreaAveragePriceProjection p = mock(AreaAveragePriceProjection.class);
        when(p.getAreaName()).thenReturn(name);
        when(p.getAveragePrice()).thenReturn(avg);
        when(p.getTransactionCount()).thenReturn(count);
        return p;
    }

    // ---------------------------------------------------------------
    // saveSalesTransactions
    // ---------------------------------------------------------------

    @Test
    void saveSalesTransactions_callsBatchUpdateOnJdbcTemplate() {
        adapter.saveSalesTransactions(List.of(domain()));

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void saveSalesTransactions_batchRowContainsCorrectValues() {
        adapter.saveSalesTransactions(List.of(domain()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());

        Object[] row = captor.getValue().get(0);
        assertThat(row[0]).isEqualTo("T001");              // trans_id
        assertThat(row[3]).isEqualTo("Downtown Dubai");    // area_name
        assertThat(row[5]).isEqualTo("Residential");       // usage
        assertThat(row[6]).isEqualTo("Sales");             // registration_type
    }

    @Test
    void saveSalesTransactions_nullTransDateConvertsToNull() {
        SaleTransaction noDate = new SaleTransaction(null, "TX", null,
                new BigDecimal("1000000"), "Area", null, null, null, null, null, null, null);

        adapter.saveSalesTransactions(List.of(noDate));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());

        assertThat(captor.getValue().get(0)[1]).isNull(); // trans_date
    }

    @Test
    void saveSalesTransactions_largeListIsBatchedInChunks() {
        // 1050 transactions → 3 batches (500, 500, 50)
        List<SaleTransaction> big = Collections.nCopies(1050, domain());

        adapter.saveSalesTransactions(big);

        verify(jdbcTemplate, times(3)).batchUpdate(anyString(), anyList());
    }

    // ---------------------------------------------------------------
    // loadSales
    // ---------------------------------------------------------------

    @Test
    void loadSales_queriesRepositoryAndMapsToDomain() {
        SalesFilter filter = new SalesFilter(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(entity())));

        Page<SaleTransaction> result = adapter.loadSales(filter, pageable);

        assertThat(result).hasSize(1);
        SaleTransaction t = result.getContent().get(0);
        assertThat(t.id()).isEqualTo(1L);
        assertThat(t.transId()).isEqualTo("T001");
        assertThat(t.transDate()).isEqualTo(LocalDate.of(2025, 3, 10));
        assertThat(t.transValue()).isEqualByComparingTo("2500000");
        assertThat(t.areaName()).isEqualTo("Downtown Dubai");
        assertThat(t.registrationType()).isEqualTo("Sales");
    }

    // ---------------------------------------------------------------
    // loadSalesAreas
    // ---------------------------------------------------------------

    @Test
    void loadSalesAreas_returnsDistinctAreaNamesFromRepository() {
        when(repository.findDistinctAreaNames()).thenReturn(List.of("Area A", "Area B"));

        assertThat(adapter.loadSalesAreas()).containsExactly("Area A", "Area B");
    }

    // ---------------------------------------------------------------
    // loadAverageSalesPrices
    // ---------------------------------------------------------------

    @Test
    void loadAverageSalesPrices_mapsProjectionsToDomainObjects() {
        AreaAveragePriceProjection proj = projection("Marina", 2000000.5, 80L);
        when(repository.findAveragePricesPerArea()).thenReturn(List.of(proj));

        List<AreaAveragePrice> result = adapter.loadAverageSalesPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).areaName()).isEqualTo("Marina");
        assertThat(result.get(0).averagePrice()).isEqualByComparingTo("2000000.50");
        assertThat(result.get(0).transactionCount()).isEqualTo(80L);
    }

    @Test
    void loadAverageSalesPrices_nullAverageMapsToZero() {
        AreaAveragePriceProjection proj = projection("Area", null, 0L);
        when(repository.findAveragePricesPerArea()).thenReturn(List.of(proj));

        assertThat(adapter.loadAverageSalesPrices().get(0).averagePrice())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------
    // loadAverageSalesPriceByArea
    // ---------------------------------------------------------------

    @Test
    void loadAverageSalesPriceByArea_foundReturnsPresent() {
        AreaAveragePriceProjection proj = projection("Jumeirah", 3500000.0, 30L);
        when(repository.findAveragePriceByArea("Jumeirah")).thenReturn(Optional.of(proj));

        Optional<AreaAveragePrice> result = adapter.loadAverageSalesPriceByArea("Jumeirah");

        assertThat(result).isPresent();
        assertThat(result.get().areaName()).isEqualTo("Jumeirah");
        assertThat(result.get().averagePrice()).isEqualByComparingTo("3500000.00");
    }

    @Test
    void loadAverageSalesPriceByArea_notFoundReturnsEmpty() {
        when(repository.findAveragePriceByArea("Ghost")).thenReturn(Optional.empty());

        assertThat(adapter.loadAverageSalesPriceByArea("Ghost")).isEmpty();
    }

    // ---------------------------------------------------------------
    // loadLastSalesByArea
    // ---------------------------------------------------------------

    @Test
    void loadLastSalesByArea_queriesWithCorrectPageableAndMapsToDomain() {
        when(repository.findLastByAreaName(eq("Marina"), any(Pageable.class)))
                .thenReturn(List.of(entity()));

        List<SaleTransaction> result = adapter.loadLastSalesByArea("Marina", 3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).transId()).isEqualTo("T001");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findLastByAreaName(eq("Marina"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
    }
}
