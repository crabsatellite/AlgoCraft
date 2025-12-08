package com.crabmods.algocraft.logic;

import org.codehaus.janino.SimpleCompiler;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CodeExecutor {
    public static String execute(String code, String input, String expectedOutput) {
        try {
            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(code);
            Class<?> clazz = compiler.getClassLoader().loadClass("Solution");
            Object instance = clazz.getDeclaredConstructor().newInstance();
            
            // Find the first public method that isn't Object's methods
            Method method = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                    method = m;
                    break;
                }
            }
            
            if (method == null) return "ERROR: No public method found in Solution class";

            Object[] args = parseArgs(input, method.getParameterTypes());
            Object result = method.invoke(instance, args);
            
            String resultStr = formatResult(result);
            
            // Normalize for comparison (remove spaces)
            String normResult = resultStr.replaceAll("\\s+", "");
            String normExpected = expectedOutput.replaceAll("\\s+", "");
            
            if (normResult.equals(normExpected)) {
                return "PASS";
            } else {
                return "FAIL: Expected " + expectedOutput + ", got " + resultStr;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static Object[] parseArgs(String input, Class<?>[] types) {
        // Very basic parser for the demo. 
        // Input format: "nums = [2,7,11,15], target = 9"
        // We assume the order matches the method arguments.
        
        String[] parts = input.split(",\\s*(?=[a-zA-Z]+)"); // Split by comma followed by var name
        Object[] args = new Object[types.length];
        
        for (int i = 0; i < types.length && i < parts.length; i++) {
            String valPart = parts[i].split("=")[1].trim();
            args[i] = parseValue(valPart, types[i]);
        }
        return args;
    }

    private static Object parseValue(String val, Class<?> type) {
        if (type == int[].class) {
            val = val.replace("[", "").replace("]", "");
            String[] nums = val.split(",");
            int[] arr = new int[nums.length];
            for (int i=0; i<nums.length; i++) arr[i] = Integer.parseInt(nums[i].trim());
            return arr;
        } else if (type == int.class) {
            return Integer.parseInt(val);
        }
        return null;
    }

    private static String formatResult(Object result) {
        if (result instanceof int[]) {
            return Arrays.toString((int[]) result);
        }
        return String.valueOf(result);
    }
}
