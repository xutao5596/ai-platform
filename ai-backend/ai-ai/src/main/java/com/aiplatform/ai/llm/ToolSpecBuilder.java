package com.aiplatform.ai.llm;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具规范构造器:把工具元信息(name/description/parametersSchema)转为 LangChain4j {@link ToolSpecification}。
 *
 * <p>为避免 ai-ai 与 ai-assistant 循环依赖,本类不直接引用 ai-assistant 的 AssistantTool 接口,
 * 而是通过 {@link ToolInfo} 抽象(由调用方注入适配器)。AssistantChatService 把 ToolRegistry 转成 List&lt;ToolInfo&gt; 再传入。</p>
 */
@Slf4j
public class ToolSpecBuilder {

    /**
     * 工具元信息抽象(解耦 AssistantTool SPI)。
     */
    public interface ToolInfo {
        String name();
        String description();
        String parametersSchema();
    }

    /**
     * 把一组工具信息转为 LangChain4j ToolSpecification 列表。
     */
    public List<ToolSpecification> buildAll(Iterable<ToolInfo> tools) {
        List<ToolSpecification> specs = new ArrayList<>();
        for (ToolInfo t : tools) {
            try {
                specs.add(build(t));
            } catch (Exception e) {
                log.warn("[ToolSpecBuilder] skip tool {}: {}", t.name(), e.getMessage());
            }
        }
        return specs;
    }

    /**
     * 构造单个 ToolSpecification。
     */
    public ToolSpecification build(ToolInfo tool) {
        JsonObjectSchema params = parseParametersSchema(tool.parametersSchema());
        return ToolSpecification.builder()
                .name(tool.name())
                .description(tool.description())
                .parameters(params)
                .build();
    }

    /**
     * 解析 AssistantTool.parametersSchema() 的自定义格式:
     *   {"url":"string,required","method":"string,default:GET,options:GET,POST,PUT,DELETE","timeout":"number,default:10000"}
     * 转为 LangChain4j JsonObjectSchema。空 schema / "{}" 视为无参。
     */
    JsonObjectSchema parseParametersSchema(String raw) {
        if (raw == null || raw.isBlank() || raw.trim().equals("{}")) {
            return null;
        }
        Map<String, String> flat;
        try {
            flat = FlatJsonParser.parseFlatStringMap(raw);
        } catch (Exception e) {
            log.warn("[ToolSpecBuilder] parametersSchema 解析失败,返回空 schema: raw={}", raw, e);
            return null;
        }
        if (flat.isEmpty()) {
            return null;
        }
        Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Map.Entry<String, String> e : flat.entrySet()) {
            String name = e.getKey();
            ParamInfo pi = ParamInfo.from(e.getValue());
            properties.put(name, toJsonSchema(pi));
            if (pi.required) {
                required.add(name);
            }
        }
        JsonObjectSchema.Builder b = JsonObjectSchema.builder().addProperties(properties);
        if (!required.isEmpty()) {
            b.required(required);
        }
        return b.build();
    }

    private JsonSchemaElement toJsonSchema(ParamInfo pi) {
        return switch (pi.type) {
            case "number", "integer" -> dev.langchain4j.model.chat.request.json.JsonNumberSchema.builder()
                    .description(pi.description()).build();
            case "boolean" -> dev.langchain4j.model.chat.request.json.JsonBooleanSchema.builder()
                    .description(pi.description()).build();
            case "object" -> JsonObjectSchema.builder().description(pi.description()).build();
            case "array" -> dev.langchain4j.model.chat.request.json.JsonArraySchema.builder()
                    .description(pi.description()).build();
            case "enum" -> dev.langchain4j.model.chat.request.json.JsonEnumSchema.builder()
                    .enumValues(pi.enumValues == null ? List.of() : pi.enumValues)
                    .description(pi.description())
                    .build();
            default -> dev.langchain4j.model.chat.request.json.JsonStringSchema.builder()
                    .description(pi.description()).build();
        };
    }

    /**
     * 参数元信息。
     */
    public static final class ParamInfo {
        public String type = "string";
        public boolean required = false;
        public String defaultValue;
        public List<String> enumValues;
        public String description;

        public String description() { return description; }

        static ParamInfo from(String spec) {
            ParamInfo p = new ParamInfo();
            if (spec == null) return p;
            List<String> parts = Arrays.stream(spec.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (!parts.isEmpty()) {
                p.type = parts.get(0).toLowerCase();
            }
            for (int i = 1; i < parts.size(); i++) {
                String tok = parts.get(i);
                if (tok.equalsIgnoreCase("required")) {
                    p.required = true;
                } else if (tok.toLowerCase().startsWith("default:")) {
                    p.defaultValue = tok.substring("default:".length());
                } else if (tok.toLowerCase().startsWith("options:")) {
                    p.enumValues = Arrays.stream(tok.substring("options:".length()).split("\\|"))
                            .map(String::trim).filter(s -> !s.isEmpty()).toList();
                    p.type = "enum";
                } else if (tok.toLowerCase().startsWith("optional(") || tok.toLowerCase().startsWith("optional")) {
                    // 忽略 optional 标记,默认非 required
                } else {
                    p.description = (p.description == null ? "" : p.description + ", ") + tok;
                }
            }
            return p;
        }
    }

    /**
     * 极简 flat-string-JSON 解析器:支持形如
     *   {"a":"x","b":"y","c":"number,required"}
     * 的对象(只支持单层,值都是字符串;与 AssistantTool.parametersSchema() 的实际写法匹配)。
     */
    static final class FlatJsonParser {
        static Map<String, String> parseFlatStringMap(String json) {
            Map<String, String> out = new LinkedHashMap<>();
            if (json == null) return out;
            String s = json.trim();
            if (s.equals("{}") || s.isEmpty()) return out;
            if (!s.startsWith("{") || !s.endsWith("}")) {
                throw new IllegalArgumentException("不是 JSON 对象: " + s);
            }
            s = s.substring(1, s.length() - 1);
            int i = 0;
            int n = s.length();
            while (i < n) {
                while (i < n && Character.isWhitespace(s.charAt(i))) i++;
                if (i >= n) break;
                if (s.charAt(i) != '"') {
                    throw new IllegalArgumentException("期望字符串 key,位置 " + i);
                }
                int keyStart = ++i;
                StringBuilder key = new StringBuilder();
                while (i < n) {
                    char c = s.charAt(i);
                    if (c == '\\' && i + 1 < n) {
                        key.append(s.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (c == '"') break;
                    key.append(c);
                    i++;
                }
                if (i >= n) throw new IllegalArgumentException("未闭合的 key");
                i++;
                while (i < n && Character.isWhitespace(s.charAt(i))) i++;
                if (i >= n || s.charAt(i) != ':') {
                    throw new IllegalArgumentException("期望 ':' 在位置 " + i);
                }
                i++;
                while (i < n && Character.isWhitespace(s.charAt(i))) i++;
                if (i >= n || s.charAt(i) != '"') {
                    throw new IllegalArgumentException("期望字符串 value 在位置 " + i);
                }
                int valStart = ++i;
                StringBuilder val = new StringBuilder();
                while (i < n) {
                    char c = s.charAt(i);
                    if (c == '\\' && i + 1 < n) {
                        val.append(s.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (c == '"') break;
                    val.append(c);
                    i++;
                }
                if (i >= n) throw new IllegalArgumentException("未闭合的 value");
                i++;
                out.put(key.toString(), val.toString());
                while (i < n && Character.isWhitespace(s.charAt(i))) i++;
                if (i < n && s.charAt(i) == ',') i++;
            }
            return out;
        }
    }
}
