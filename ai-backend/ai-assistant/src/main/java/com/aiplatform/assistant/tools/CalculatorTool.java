package com.aiplatform.assistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数学计算工具(支持 + - * / ^ 括号和数字字面量)。
 * PoC:自实现的简易表达式求值(Shunting-yard)。
 * 替代方案:用 Spring Expression Language(SpEL)或 ScriptEngine。
 */
@Slf4j
@Component
public class CalculatorTool implements AssistantTool {

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String displayName() {
        return "数学计算";
    }

    @Override
    public String description() {
        return "计算数学表达式,支持 + - * / ^ 括号与数字,例如:'(1+2)*3^2'。";
    }

    @Override
    public String category() {
        return "工具";
    }

    @Override
    public String parametersSchema() {
        return "{\"expression\":\"string,required\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        String expr = (String) args.get("expression");
        if (expr == null || expr.isBlank()) return ToolResult.fail("expression 不能为空");
        try {
            double result = evaluate(expr);
            String output = String.format("%s = %s", expr.trim(), formatResult(result));
            return ToolResult.ok(output);
        } catch (Exception e) {
            log.warn("CalculatorTool error: expr={}", expr, e);
            return ToolResult.fail("计算失败: " + e.getMessage());
        }
    }

    /**
     * Shunting-yard 实现:中缀 → 后缀 → 求值。
     * 简化:仅支持数字、+ - * / ^、( )和一元负号。
     */
    static double evaluate(String expr) {
        java.util.List<String> rpn = toRPN(expr);
        java.util.Deque<Double> stack = new java.util.ArrayDeque<>();
        for (String token : rpn) {
            if (token.matches("-?\\d+(\\.\\d+)?")) {
                stack.push(Double.parseDouble(token));
            } else {
                double b = stack.pop();
                double a = stack.isEmpty() ? 0 : stack.pop();
                stack.push(apply(a, b, token));
            }
        }
        if (stack.isEmpty()) throw new IllegalArgumentException("空表达式");
        return stack.pop();
    }

    private static double apply(double a, double b, String op) {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            case "^" -> Math.pow(a, b);
            default -> throw new IllegalArgumentException("不支持的运算符: " + op);
        };
    }

    static java.util.List<String> toRPN(String expr) {
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.Deque<String> ops = new java.util.ArrayDeque<>();
        StringBuilder num = new StringBuilder();
        boolean lastWasOp = true;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) continue;
            if (Character.isDigit(c) || c == '.') {
                num.append(c);
                lastWasOp = false;
            } else {
                if (num.length() > 0) {
                    out.add(num.toString());
                    num.setLength(0);
                }
                if (c == '(') {
                    ops.push("(");
                } else if (c == ')') {
                    while (!ops.isEmpty() && !ops.peek().equals("(")) out.add(ops.pop());
                    if (ops.isEmpty()) throw new IllegalArgumentException("括号不匹配");
                    ops.pop();
                } else {
                    String op = String.valueOf(c);
                    if (op.equals("-") && lastWasOp) {
                        // 一元负号 → 0 - x
                        out.add("0");
                    }
                    while (!ops.isEmpty() && !ops.peek().equals("(") && precedence(ops.peek()) >= precedence(op)) {
                        out.add(ops.pop());
                    }
                    ops.push(op);
                    lastWasOp = true;
                }
            }
        }
        if (num.length() > 0) out.add(num.toString());
        while (!ops.isEmpty()) {
            String t = ops.pop();
            if (t.equals("(")) throw new IllegalArgumentException("括号不匹配");
            out.add(t);
        }
        return out;
    }

    private static int precedence(String op) {
        return switch (op) {
            case "+", "-" -> 1;
            case "*", "/" -> 2;
            case "^" -> 3;
            default -> 0;
        };
    }

    private String formatResult(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format("%.6f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
