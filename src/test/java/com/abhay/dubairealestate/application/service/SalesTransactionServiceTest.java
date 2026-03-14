package com.abhay.dubairealestate.application.service;

import com.abhay.dubairealestate.application.port.in.SalesFilter;
import com.abhay.dubairealestate.application.port.out.LoadSalesTransactionsPort;
import com.abhay.dubairealestate.application.port.out.ParseSalesCsvPort;
import com.abhay.dubairealestate.application.port.out.SaveSalesTransactionsPort;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesTransactionService}.
 * All external ports are mocked; this verifies orchestration logic only.
 */
@ExtendWith(MockitoExtension.class)
class SalesTransactionServiceTest {

    @Mock private ParseSalesCsvPort parseSalesCsvPort;
    @Mock private SaveSalesTransactionsPort saveSalesTransactionsPort;
    @Mock private LoadSalesTransactionsPort loadSalesTransactionsPort;

    @InjectMocks private SalesTransactionService service;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private SaleTransaction sale(Long id) {
        return new SaleTransaction(id, "T00" + id, LocalDate.of(2025, 3, 1),
                new BigDecimal("2000000"), "Downtown Dubai", "Emaar Tower", "Residential",
                "Sales", "Apartment", "2 B/R", new BigDecimal("120"),
                new BigDecimal("16666.67"));
    }

    // ---------------------------------------------------------------
    // uploadSales
    // ---------------------------------------------------------------

    @Test
    void uploadSales_parsesStreamSavesTransactionsAndReturnsCount() {
        InputStream stream = mock(InputStream.class);
        List<SaleTransaction> parsed = List.of(sale(null), sale(null), sale(null));
        when(parseSalesCsvPort.parseSalesCsv(stream)).thenReturn(parsed);

        int count = service.uploadSales(stream);

        assertThat(count).isEqualTo(3);
        verify(saveSalesTransactionsPort).saveSalesTransactions(parsed);
    }

    @Test
    void uploadSales_emptyParseResultSavesEmptyListAndReturnsZero() {
        InputStream stream = mock(InputStream.class);
        when(parseSalesCsvPort.parseSalesCsv(stream)).thenReturn(List.of());

        int count = service.uploadSales(stream);

        assertThat(count).isZero();
        verify(saveSalesTransactionsPort).saveSalesTransactions(List.of());
    }

    @Test
    void uploadSales_saveIsCalledExactlyOnce() {
        InputStream stream = mock(InputStream.class);
        when(parseSalesCsvPort.parseSalesCsv(stream)).thenReturn(List.of(sale(null)));

        service.uploadSales(stream);

        verify(saveSalesTransactionsPort, times(1)).saveSalesTransactions(any());
    }

    // ---------------------------------------------------------------
    // querySales
    // ---------------------------------------------------------------

    @Test
    void querySales_delegatesToLoadPortAndReturnsResult() {
        SalesFilter filter = new SalesFilter("Residential", null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<SaleTransaction> expected = new PageImpl<>(List.of(sale(1L)));
        when(loadSalesTransactionsPort.loadSales(filter, pageable)).thenReturn(expected);

        Page<SaleTransaction> result = service.querySales(filter, pageable);

        assertThat(result).isEqualTo(expected);
        verify(loadSalesTransactionsPort).loadSales(filter, pageable);
    }

    @Test
    void querySales_withPriceFilterAndPagination_returnsCorrectPage() {
        SalesFilter filter = new SalesFilter(null, new BigDecimal("1000000"),
                new BigDecimal("5000000"), null, "Emaar");
        Pageable pageable = PageRequest.of(2, 5);
        Page<SaleTransaction> expected = new PageImpl<>(List.of(sale(10L)), pageable, 100);
        when(loadSalesTransactionsPort.loadSales(filter, pageable)).thenReturn(expected);

        Page<SaleTransaction> result = service.querySales(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(100);
        assertThat(result.getNumber()).isEqualTo(2);
    }

    // ---------------------------------------------------------------
    // getSalesAreas
    // ---------------------------------------------------------------

    @Test
    void getSalesAreas_delegatesToLoadPortAndReturnsList() {
        List<String> areas = List.of("Area1", "Area2", "Area3");
        when(loadSalesTransactionsPort.loadSalesAreas()).thenReturn(areas);

        assertThat(service.getSalesAreas()).isEqualTo(areas);
    }

    @Test
    void getSalesAreas_emptyDatabaseReturnsEmptyList() {
        when(loadSalesTransactionsPort.loadSalesAreas()).thenReturn(List.of());

        assertThat(service.getSalesAreas()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getAverageSalesPrices
    // ---------------------------------------------------------------

    @Test
    void getAverageSalesPrices_delegatesToLoadPortAndReturnsList() {
        List<AreaAveragePrice> averages = List.of(
                new AreaAveragePrice("Downtown Dubai", new BigDecimal("2500000.00"), 120L)
        );
        when(loadSalesTransactionsPort.loadAverageSalesPrices()).thenReturn(averages);

        assertThat(service.getAverageSalesPrices()).isEqualTo(averages);
    }

    // ---------------------------------------------------------------
    // getAverageSalesPriceByArea
    // ---------------------------------------------------------------

    @Test
    void getAverageSalesPriceByArea_existingArea_returnsPresent() {
        AreaAveragePrice avg = new AreaAveragePrice("Marina", new BigDecimal("1800000.00"), 75L);
        when(loadSalesTransactionsPort.loadAverageSalesPriceByArea("Marina"))
                .thenReturn(Optional.of(avg));

        Optional<AreaAveragePrice> result = service.getAverageSalesPriceByArea("Marina");

        assertThat(result).isPresent().contains(avg);
    }

    @Test
    void getAverageSalesPriceByArea_unknownArea_returnsEmpty() {
        when(loadSalesTransactionsPort.loadAverageSalesPriceByArea("Unknown"))
                .thenReturn(Optional.empty());

        assertThat(service.getAverageSalesPriceByArea("Unknown")).isEmpty();
    }

    // ---------------------------------------------------------------
    // getLastSalesByArea
    // ---------------------------------------------------------------

    @Test
    void getLastSalesByArea_delegatesToPortWithCorrectArguments() {
        List<SaleTransaction> expected = List.of(sale(1L), sale(2L));
        when(loadSalesTransactionsPort.loadLastSalesByArea("Marina", 5))
                .thenReturn(expected);

        List<SaleTransaction> result = service.getLastSalesByArea("Marina", 5);

        assertThat(result).hasSize(2);
        verify(loadSalesTransactionsPort).loadLastSalesByArea("Marina", 5);
    }

    @Test
    void getLastSalesByArea_noDataForArea_returnsEmptyList() {
        when(loadSalesTransactionsPort.loadLastSalesByArea("Ghost Area", 10))
                .thenReturn(List.of());

        assertThat(service.getLastSalesByArea("Ghost Area", 10)).isEmpty();
    }
}
