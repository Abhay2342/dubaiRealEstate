package com.abhay.dubairealestate.configuration;

import com.abhay.dubairealestate.infrastructure.mcp.RealEstateMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that wires infrastructure adapters to application ports.
 *
 * Port → Adapter bindings are resolved automatically via @Component / @Service scanning
 * since each port has exactly one implementation. This class exists as the explicit
 * "glue" layer for cross-cutting concerns and for registering the MCP tool provider.
 */
@Configuration
public class BeanConfiguration {

    /**
     * Registers all @Tool-annotated methods on {@link RealEstateMcpTools}
     * with the Spring AI MCP server so they are discoverable by MCP clients.
     */
    @Bean
    public ToolCallbackProvider realEstateMcpToolCallbackProvider(RealEstateMcpTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
