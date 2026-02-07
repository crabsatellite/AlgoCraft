package com.crabmods.algocraft.testengine.adapters;

import com.crabmods.algocraft.testengine.*;

import java.lang.reflect.*;

/**
 * Test adapter for Codec (serialize/deserialize or encode/decode) problems.
 * Calls serialize then deserialize (or encode then decode) and verifies round-trip.
 */
public class CodecAdapter {

    public static TestResult run(Class<?> codecClass, String input, String expected) {
        try {
            Constructor<?> ctor = codecClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object codec = ctor.newInstance();

            // Find serialize/encode and deserialize/decode methods
            Method serializeMethod = findMethod(codecClass, "serialize", "encode");
            Method deserializeMethod = findMethod(codecClass, "deserialize", "decode");

            if (serializeMethod == null || deserializeMethod == null) {
                return TestResult.error("Codec class missing serialize/deserialize methods");
            }
            serializeMethod.setAccessible(true);
            deserializeMethod.setAccessible(true);

            // Parse input (e.g., "root = [1,2,3,null,null,4,5]")
            Object[] serializeArgs = InputParser.parseInput(input, serializeMethod);
            Object serialized = serializeMethod.invoke(codec, serializeArgs);

            // Deserialize
            Object deserialized = deserializeMethod.invoke(codec, serialized);

            // Format and compare output
            String actualStr = OutputComparator.format(deserialized);
            Class<?> returnType = deserializeMethod.getReturnType();

            boolean passed = OutputComparator.compare(actualStr, expected, returnType);
            if (passed) {
                return TestResult.pass();
            } else {
                return TestResult.fail(input, expected, actualStr);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            String msg = cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                    : e.getMessage();
            return TestResult.error(msg);
        } catch (Exception e) {
            return TestResult.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Method findMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
        }
        return null;
    }
}
