package com.aiplatform.assistant.tools;

import com.aiplatform.assistant.dto.ToolDefinition;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心:启动时扫描所有实现 AssistantTool 接口的 Spring Bean,按 name() 索引。
 * PoC 版:简单的内存注册;后续可支持热加载/插件化。
 */
@Slf4j
@Component
public class ToolRegistry {

    private final ApplicationContext applicationContext;
    private final Map<String, AssistantTool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        Collection<AssistantTool> beans = applicationContext.getBeansOfType(AssistantTool.class).values();
        for (AssistantTool tool : beans) {
            tools.put(tool.name(), tool);
            log.info("[ToolRegistry] Registered tool: {} ({})", tool.name(), tool.displayName());
        }
        log.info("[ToolRegistry] Total {} tools loaded.", tools.size());
    }

    public AssistantTool get(String name) {
        return tools.get(name);
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    public Collection<AssistantTool> all() {
        return tools.values();
    }

    public List<ToolDefinition> definitions() {
        List<ToolDefinition> defs = new ArrayList<>();
        for (AssistantTool t : tools.values()) {
            ToolDefinition d = new ToolDefinition();
            d.setName(t.name());
            d.setDisplayName(t.displayName());
            d.setDescription(t.description());
            d.setCategory(t.category());
            d.setParametersSchema(t.parametersSchema());
            d.setEnabled(true);
            defs.add(d);
        }
        return defs;
    }

    /**
     * 刷新注册(用于动态添加的工具)。
     */
    public void refresh() {
        tools.clear();
        init();
    }
}
