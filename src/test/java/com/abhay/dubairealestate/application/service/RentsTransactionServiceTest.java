package com.abhay.dubairealestate.application.service;

import com.abhay.dubairealestate.application.port.in.RentsFilter;
import com.abhay.dubairealestate.application.port.out.LoadRentsTransactionsPort;
import com.abhay.dubairealestate.application.port.out.ParseRentsCsvPort;
import com.abhay.dubairealestate.application.port.out.SaveRentsTransactionsPort;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
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
 * Unit tests for {@link RentsTransactionService}.
 * All external ports are mocked; this verifies orchestration logic only.
 */
@ExtendWith(MockitoExtension.class)
class RentsTransactionServiceTest {

    @Mock private ParseRentsCsvPort parseRentsCsvPort;
    @Mock private SaveRentsTransactionsPort saveRentsTransactionsPort;
    @Mock private LoadRentsTransactionsPort loadRentsTransactionsPort;

    @InjectMocks private RentsTransactionService service;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private RentTransaction rent(Long id) {
        return new RentTransaction(id, LocalDate.of(2025, 1, 1),
                new BigDecimal("50000"), LocalDate.of(2025, 2, 1), LocalDate.of(2026, 1, 31),
                "Downtown Dubai", "BK Tower", "Residential", "Apartment", "1 B/R",
                new BigDecimal("80"));
    }

    // ---------------------------------------------------------------
    // uploadRents
    // ---------------------------------------------------------------

    @Test
    void uploadRents_parsesStreamSavesTransactionsAndReturnsCount() {
        InputStream stream = mock(InputStream.class);
        List<RentTransaction> parsed = List.of(rent(null), rent(null));
        when(parseRentsCsvPort.parseRentsCsv(stream)).thenReturn(parsed);

        int count = service.uploadRents(stream);

        assertThat(count).isEqualTo(2);
        verify(saveRentsTransactionsPort).saveRentsTransactions(parsed);
    }

    @Test
    void uploadRents_emptyParseResultSavesEmptyListAndReturnsZero() {
        InputStream stream = mock(InputStream.class);
        when(parseRentsCsvPort.parseRentsCsv(stream)).thenReturn(List.of());

        int count = service.uploadRents(stream);

        assertThat(count).isZero();
        verify(saveRentsTransactionsPort).saveRentsTransactions(List.of());
    }

    @Test
    void uploadRents_saveIsCalledExactlyOnce() {
        InputStream stream = mock(InputStream.class);
        when(parseRentsCsvPort.parseRentsCsv(stream)).thenReturn(List.of(rent(null)));

        service.uploadRents(stream);

        verify(saveRentsTransactionsPort, times(1)).saveRentsTransactions(any());
    }

    // ---------------------------------------------------------------
    // queryRents
    // ---------------------------------------------------------------

    @Test
    void queryRents_delegatesToLoadPortAndReturnsResult() {
        RentsFilter filter = new RentsFilter(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<RentTransaction> expected = new PageImpl<>(List.of(rent(1L)));
        when(loadRentsTransactionsPort.loadRents(filter, pageable)).thenReturn(expected);

        Page<RentTransaction> result = service.queryRents(filter, pageable);

        assertThat(result).isEqualTo(expected);
        verify(loadRentsTransactionsPort).loadRents(filter, pageable);
    }

    @Test
    void queryRents_withFilteredResults_returnsFilteredPage() {
        RentsFilter filter = new RentsFilter("Residential", new BigDecimal("40000"),
                new BigDecimal("80000"), "Downtown", null);
        Pageable pageable = PageRequest.of(1, 10);
        Page<RentTransaction> expected = new PageImpl<>(List.of(rent(5L)), pageable, 50);
        when(loadRentsTransactionsPort.loadRents(filter, pageable)).thenReturn(expected);

        Page<RentTransaction> result = service.queryRents(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(50);
        assertThat(result.getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------
    // getRentsAreas
    // ---------------------------------------------------------------

    @Test
    void getRentsAreas_delegatesToLoadPortAndReturnsList() {
        List<String> areas = List.of("Downtown Dubai", "Jumeirah", "Marina");
        when(loadRentsTransactionsPort.loadRentsAreas()).thenReturn(areas);

        assertThat(service.getRentsAreas()).isEqualTo(areas);
        verify(loadRentsTransactionsPort).loadRentsAreas();
    }

    @Test
    void getRentsAreas_emptyDatabaseReturnsEmptyList() {
        when(loadRentsTransactionsPort.loadRentsAreas()).thenReturn(List.of());

        assertThat(service.getRentsAreas()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getAverageRentsPrices
    // ---------------------------------------------------------------

    @Test
    void getAverageRentsPrices_delegatesToLoadPortAndReturnsList() {
        List<AreaAveragePrice> averages = List.of(
                new AreaAveragePrice("Downtown", new BigDecimal("75000.00"), 100L),
                new AreaAveragePrice("Marina", new BigDecimal("55000.00"), 200L)
        );
        when(loadRentsTransactionsPort.loadAverageRentsPrices()).thenReturn(averages);

        assertThat(service.getAverageRentsPrices()).isEqualTo(averages);
    }

    // ---------------------------------------------------------------
    // getAverageRentsPriceByArea
    // ---------------------------------------------------------------

    @Test
    void getAverageRentsPriceByArea_existingArea_returnsPresent() {
        AreaAveragePrice avg = new AreaAveragePrice("Downtown Dubai", new BigDecimal("72000.00"), 340L);
        when(loadRentsTransactionsPort.loadAverageRentsPriceByArea("Downtown Dubai"))
                .thenReturn(Optional.of(avg));

        Optional<AreaAveragePrice> result = service.getAverageRentsPriceByArea("Downtown Dubai");

        assertThat(result).isPresent().contains(avg);
    }

    @Test
    void getAverageRentsPriceByArea_unknownArea_returnsEmpty() {
        when(loadRentsTransactionsPort.loadAverageRentsPriceByArea("Unknown"))
                .thenReturn(Optional.empty());

        assertThat(service.getAverageRentsPriceByArea("Unknown")).isEmpty();
    }

    // ---------------------------------------------------------------
    // getLastRentsByArea
    // ---------------------------------------------------------------

    @Test
    void getLastRentsByArea_delegatesToPortWithCorrectArguments() {
        List<RentTransaction> expected = List.of(rent(1L), rent(2L));
        when(loadRentsTransactionsPort.loadLastRentsByArea("Downtown Dubai", 10))
                .thenReturn(expected);

        List<RentTransaction> result = service.getLastRentsByArea("Downtown Dubai", 10);

        assertThat(result).hasSize(2);
        verify(loadRentsTransactionsPort).loadLastRentsByArea("Downtown Dubai", 10);
    }

    @Test
    void getLastRentsByArea_noDataForArea_returnsEmptyList() {
        when(loadRentsTransactionsPort.loadLastRentsByArea("NonExistent", 5))
                .thenReturn(List.of());

        assertThat(service.getLastRentsByArea("NonExistent", 5)).isEmpty();
    }
}
