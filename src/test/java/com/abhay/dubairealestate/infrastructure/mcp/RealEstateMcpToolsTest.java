package com.abhay.dubairealestate.infrastructure.mcp;

import com.abhay.dubairealestate.application.port.in.*;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RealEstateMcpTools}.
 * All use-case collaborators are mocked to verify tool response composition.
 */
@ExtendWith(MockitoExtension.class)
class RealEstateMcpToolsTest {

    @Mock private QuerySalesAreasUseCase querySalesAreasUseCase;
    @Mock private QueryRentsAreasUseCase queryRentsAreasUseCase;
    @Mock private QueryLastSalesTransactionsUseCase queryLastSalesTransactionsUseCase;
    @Mock private QueryLastRentsTransactionsUseCase queryLastRentsTransactionsUseCase;
    @Mock private QuerySalesAveragePricesUseCase querySalesAveragePricesUseCase;
    @Mock private QueryRentsAveragePricesUseCase queryRentsAveragePricesUseCase;

    @InjectMocks private RealEstateMcpTools mcpTools;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private SaleTransaction sale() {
        return new SaleTransaction(1L, "T001", LocalDate.of(2025, 3, 1),
                new BigDecimal("2500000"), "Downtown Dubai", "Emaar Tower",
                "Residential", "Sales", "Apartment", "2 B/R",
                new BigDecimal("120"), new BigDecimal("20833.33"));
    }

    private SaleTransaction saleWithNulls() {
        return new SaleTransaction(2L, null, null, null, null, null, null, null, null, null, null, null);
    }

    private RentTransaction rent() {
        return new RentTransaction(2L, LocalDate.of(2025, 1, 15),
                new BigDecimal("60000"), LocalDate.of(2025, 2, 1), LocalDate.of(2026, 1, 31),
                "Downtown Dubai", "Burj Khalifa", "Residential", "Apartment", "1 B/R",
                new BigDecimal("75"));
    }

    private RentTransaction rentWithNulls() {
        return new RentTransaction(3L, null, null, null, null, null, null, null, null, null, null);
    }

    // ---------------------------------------------------------------
    // getSalesAreas
    // ---------------------------------------------------------------

    @Test
    void getSalesAreas_returnsListFromUseCase() {
        List<String> areas = List.of("Downtown Dubai", "Marina", "Jumeirah");
        when(querySalesAreasUseCase.getSalesAreas()).thenReturn(areas);

        assertThat(mcpTools.getSalesAreas()).isEqualTo(areas);
    }

    @Test
    void getSalesAreas_emptyListWhenNoneExist() {
        when(querySalesAreasUseCase.getSalesAreas()).thenReturn(List.of());

        assertThat(mcpTools.getSalesAreas()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getRentsAreas
    // ---------------------------------------------------------------

    @Test
    void getRentsAreas_returnsListFromUseCase() {
        List<String> areas = List.of("Deira", "Bur Dubai");
        when(queryRentsAreasUseCase.getRentsAreas()).thenReturn(areas);

        assertThat(mcpTools.getRentsAreas()).isEqualTo(areas);
    }

    // ---------------------------------------------------------------
    // getLastTransactionsByArea
    // ---------------------------------------------------------------

    @Test
    void getLastTransactionsByArea_returnsMapWithAreaSalesAndRents() {
        when(queryLastSalesTransactionsUseCase.getLastSalesByArea("Downtown Dubai", 10))
                .thenReturn(List.of(sale()));
        when(queryLastRentsTransactionsUseCase.getLastRentsByArea("Downtown Dubai", 10))
                .thenReturn(List.of(rent()));

        Map<String, Object> result = mcpTools.getLastTransactionsByArea("Downtown Dubai");

        assertThat(result).containsKey("area").containsKey("lastSales").containsKey("lastRents");
        assertThat(result.get("area")).isEqualTo("Downtown Dubai");
    }

    @Test
    void getLastTransactionsByArea_salesMapContainsExpectedFields() {
        when(queryLastSalesTransactionsUseCase.getLastSalesByArea(anyString(), eq(10)))
                .thenReturn(List.of(sale()));
        when(queryLastRentsTransactionsUseCase.getLastRentsByArea(anyString(), eq(10)))
                .thenReturn(List.of());

        Map<String, Object> result = mcpTools.getLastTransactionsByArea("Downtown Dubai");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sales = (List<Map<String, Object>>) result.get("lastSales");
        assertThat(sales).hasSize(1);
        Map<String, Object> saleMap = sales.get(0);
        assertThat(saleMap).containsEntry("areaName", "Downtown Dubai")
                .containsEntry("propertyType", "Apartment")
                .containsEntry("rooms", "2 B/R")
                .containsEntry("transValue", "2500000");
        assertThat(saleMap.get("transDate")).isEqualTo("2025-03-01");
    }

    @Test
    void getLastTransactionsByArea_rentsMapContainsExpectedFields() {
        when(queryLastSalesTransactionsUseCase.getLastSalesByArea(anyString(), eq(10)))
                .thenReturn(List.of());
        when(queryLastRentsTransactionsUseCase.getLastRentsByArea(anyString(), eq(10)))
                .thenReturn(List.of(rent()));

        Map<String, Object> result = mcpTools.getLastTransactionsByArea("Downtown Dubai");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rents = (List<Map<String, Object>>) result.get("lastRents");
        assertThat(rents).hasSize(1);
        Map<String, Object> rentMap = rents.get(0);
        assertThat(rentMap).containsEntry("areaName", "Downtown Dubai")
                .containsEntry("propertyType", "Apartment")
                .containsEntry("annualAmount", "60000");
    }

    @Test
    void getLastTransactionsByArea_nullFieldsInTransactionAreReplacedWithEmptyString() {
        when(queryLastSalesTransactionsUseCase.getLastSalesByArea(anyString(), eq(10)))
                .thenReturn(List.of(saleWithNulls()));
        when(queryLastRentsTransactionsUseCase.getLastRentsByArea(anyString(), eq(10)))
                .thenReturn(List.of(rentWithNulls()));

        Map<String, Object> result = mcpTools.getLastTransactionsByArea("Area");

        @SuppressWarnings("unchecked")
        Map<String, Object> saleMap = ((List<Map<String, Object>>) result.get("lastSales")).get(0);
        assertThat(saleMap.get("areaName")).isEqualTo("");
        assertThat(saleMap.get("transValue")).isEqualTo("");

        @SuppressWarnings("unchecked")
        Map<String, Object> rentMap = ((List<Map<String, Object>>) result.get("lastRents")).get(0);
        assertThat(rentMap.get("areaName")).isEqualTo("");
        assertThat(rentMap.get("annualAmount")).isEqualTo("");
    }

    @Test
    void getLastTransactionsByArea_emptyResultsWhenAreaHasNoData() {
        when(queryLastSalesTransactionsUseCase.getLastSalesByArea(anyString(), eq(10)))
                .thenReturn(List.of());
        when(queryLastRentsTransactionsUseCase.getLastRentsByArea(anyString(), eq(10)))
                .thenReturn(List.of());

        Map<String, Object> result = mcpTools.getLastTransactionsByArea("Unknown Area");

        @SuppressWarnings("unchecked")
        List<?> sales = (List<?>) result.get("lastSales");
        @SuppressWarnings("unchecked")
        List<?> rents = (List<?>) result.get("lastRents");
        assertThat(sales).isEmpty();
        assertThat(rents).isEmpty();
    }

    // ---------------------------------------------------------------
    // getAreaAveragePrices
    // ---------------------------------------------------------------

    @Test
    void getAreaAveragePrices_bothPresentReturnsCorrectValues() {
        when(querySalesAveragePricesUseCase.getAverageSalesPriceByArea("Downtown Dubai"))
                .thenReturn(Optional.of(new AreaAveragePrice("Downtown Dubai", new BigDecimal("2500000"), 200L)));
        when(queryRentsAveragePricesUseCase.getAverageRentsPriceByArea("Downtown Dubai"))
                .thenReturn(Optional.of(new AreaAveragePrice("Downtown Dubai", new BigDecimal("65000"), 150L)));

        Map<String, Object> result = mcpTools.getAreaAveragePrices("Downtown Dubai");

        assertThat(result.get("area")).isEqualTo("Downtown Dubai");
        assertThat(result.get("averageSalesPrice")).isEqualTo(new BigDecimal("2500000"));
        assertThat(result.get("salesTransactionCount")).isEqualTo(200L);
        assertThat(result.get("averageAnnualRent")).isEqualTo(new BigDecimal("65000"));
        assertThat(result.get("rentTransactionCount")).isEqualTo(150L);
    }

    @Test
    void getAreaAveragePrices_neitherPresentReturnsZerosAndZeroCounts() {
        when(querySalesAveragePricesUseCase.getAverageSalesPriceByArea("Unknown"))
                .thenReturn(Optional.empty());
        when(queryRentsAveragePricesUseCase.getAverageRentsPriceByArea("Unknown"))
                .thenReturn(Optional.empty());

        Map<String, Object> result = mcpTools.getAreaAveragePrices("Unknown");

        assertThat(result.get("averageSalesPrice")).isEqualTo(BigDecimal.ZERO);
        assertThat(result.get("salesTransactionCount")).isEqualTo(0L);
        assertThat(result.get("averageAnnualRent")).isEqualTo(BigDecimal.ZERO);
        assertThat(result.get("rentTransactionCount")).isEqualTo(0L);
    }

    @Test
    void getAreaAveragePrices_onlySalesPresentRentFallsBackToZero() {
        when(querySalesAveragePricesUseCase.getAverageSalesPriceByArea("Area"))
                .thenReturn(Optional.of(new AreaAveragePrice("Area", new BigDecimal("1000000"), 10L)));
        when(queryRentsAveragePricesUseCase.getAverageRentsPriceByArea("Area"))
                .thenReturn(Optional.empty());

        Map<String, Object> result = mcpTools.getAreaAveragePrices("Area");

        assertThat(result.get("averageSalesPrice")).isEqualTo(new BigDecimal("1000000"));
        assertThat(result.get("averageAnnualRent")).isEqualTo(BigDecimal.ZERO);
    }
}
