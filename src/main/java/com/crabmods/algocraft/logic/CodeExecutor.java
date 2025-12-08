package com.crabmods.algocraft.logic;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class CodeExecutor {
    public static String execute(String code, String input, String expectedOutput) {
        Path tempDir = null;
        try {
            // Use standard Java Compiler API
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return "ERROR: Java Compiler not found. Please run with a JDK, not a JRE.";
            }

            tempDir = Files.createTempDirectory("algocraft_exec_");
            
            // Write source code to file
            // Assuming the class name in 'code' is 'Solution'
            File sourceFile = new File(tempDir.toFile(), "Solution.java");
            Files.writeString(sourceFile.toPath(), code);

            // Compile
            // -d sets the output directory
            int result = compiler.run(null, null, null, "-d", tempDir.toString(), sourceFile.getPath());
            
            if (result != 0) {
                return "ERROR: Compilation failed";
            }

            // Load class
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{tempDir.toUri().toURL()});
            Class<?> clazz = Class.forName("Solution", true, classLoader);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            
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
            method.setAccessible(true);
            Object invokeResult = method.invoke(instance, args);
            
            String resultStr = formatResult(invokeResult);
            
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
        } finally {
            // Cleanup temp dir
            if (tempDir != null) {
                try {
                    File dir = tempDir.toFile();
                    File[] files = dir.listFiles();
                    if (files != null) for (File f : files) f.delete();
                    dir.delete();
                } catch (Exception ignored) {}
            }
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
