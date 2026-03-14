package com.abhay.dubairealestate.infrastructure.mcp;

import com.abhay.dubairealestate.application.port.in.*;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool definitions for the Dubai Real Estate backend.
 *
 * Exposed tools:
 *  1. getSalesAreas            – list all areas with sales transactions
 *  2. getRentsAreas            – list all areas with rent transactions
 *  3. getLastTransactionsByArea – last N transactions (sales + rents) for an area
 *  4. getAreaAveragePrices     – average sales and rent prices for an area
 */
@Component
@RequiredArgsConstructor
public class RealEstateMcpTools {

    private static final int DEFAULT_LAST_TRANSACTIONS_LIMIT = 10;

    private final QuerySalesAreasUseCase querySalesAreasUseCase;
    private final QueryRentsAreasUseCase queryRentsAreasUseCase;
    private final QueryLastSalesTransactionsUseCase queryLastSalesTransactionsUseCase;
    private final QueryLastRentsTransactionsUseCase queryLastRentsTransactionsUseCase;
    private final QuerySalesAveragePricesUseCase querySalesAveragePricesUseCase;
    private final QueryRentsAveragePricesUseCase queryRentsAveragePricesUseCase;

    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = "Get the list of all distinct area names that have sales transactions in the Dubai DLD database.")
    public List<String> getSalesAreas() {
        return querySalesAreasUseCase.getSalesAreas();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = "Get the list of all distinct area names that have rent transactions in the Dubai DLD database.")
    public List<String> getRentsAreas() {
        return queryRentsAreasUseCase.getRentsAreas();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Get the most recent property transactions (both sales and rents) for a specific area.
            Returns up to 10 of the latest sales and up to 10 of the latest rent transactions.
            """)
    public Map<String, Object> getLastTransactionsByArea(String areaName) {
        List<SaleTransaction> sales =
                queryLastSalesTransactionsUseCase.getLastSalesByArea(areaName, DEFAULT_LAST_TRANSACTIONS_LIMIT);
        List<RentTransaction> rents =
                queryLastRentsTransactionsUseCase.getLastRentsByArea(areaName, DEFAULT_LAST_TRANSACTIONS_LIMIT);

        Map<String, Object> result = new HashMap<>();
        result.put("area", areaName);
        result.put("lastSales", sales.stream().map(this::saleToMap).toList());
        result.put("lastRents", rents.stream().map(this::rentToMap).toList());
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Get the average sales price and average annual rent price for a given area.
            Returns average trans_value (sales) and average annual_amount (rents) along with transaction counts.
            """)
    public Map<String, Object> getAreaAveragePrices(String areaName) {
        var salesAvg = querySalesAveragePricesUseCase.getAverageSalesPriceByArea(areaName);
        var rentsAvg = queryRentsAveragePricesUseCase.getAverageRentsPriceByArea(areaName);

        Map<String, Object> result = new HashMap<>();
        result.put("area", areaName);
        result.put("averageSalesPrice", salesAvg.map(AreaAveragePrice::averagePrice).orElse(BigDecimal.ZERO));
        result.put("salesTransactionCount", salesAvg.map(AreaAveragePrice::transactionCount).orElse(0L));
        result.put("averageAnnualRent", rentsAvg.map(AreaAveragePrice::averagePrice).orElse(BigDecimal.ZERO));
        result.put("rentTransactionCount", rentsAvg.map(AreaAveragePrice::transactionCount).orElse(0L));
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> saleToMap(SaleTransaction s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.id());
        m.put("transDate", String.valueOf(s.transDate()));
        m.put("transValue", s.transValue() != null ? s.transValue().toPlainString() : "");
        m.put("areaName", nullSafe(s.areaName()));
        m.put("projectName", nullSafe(s.projectName()));
        m.put("propertyType", nullSafe(s.propertyType()));
        m.put("rooms", nullSafe(s.rooms()));
        return m;
    }

    private Map<String, Object> rentToMap(RentTransaction r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.id());
        m.put("contractDate", String.valueOf(r.contractDate()));
        m.put("annualAmount", r.annualAmount() != null ? r.annualAmount().toPlainString() : "");
        m.put("areaName", nullSafe(r.areaName()));
        m.put("projectName", nullSafe(r.projectName()));
        m.put("propertyType", nullSafe(r.propertyType()));
        m.put("rooms", nullSafe(r.rooms()));
        return m;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
