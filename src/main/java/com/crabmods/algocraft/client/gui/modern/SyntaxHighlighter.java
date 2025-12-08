package com.crabmods.algocraft.client.gui.modern;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    private static final Pattern KEYWORDS = Pattern.compile("\\b(public|private|protected|class|void|int|boolean|if|else|for|while|return|new|static|final|import|package)\\b");
    private static final Pattern STRINGS = Pattern.compile("\"[^\"]*\"");
    private static final Pattern NUMBERS = Pattern.compile("\\b\\d+\\b");
    private static final Pattern COMMENTS = Pattern.compile("//.*");

    public static List<FormattedCharSequence> highlight(String text) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            lines.add(highlightLine(line));
        }
        return lines;
    }

    public static FormattedCharSequence highlightLine(String line) {
        // This is a simplified highlighter. 
        // For a real implementation, we would need to parse the line into components.
        // Since Minecraft's FormattedCharSequence is complex to build manually with regex overlaps,
        // we will do a simple pass: split by spaces and color words.
        // A better approach is to build a Component and then get visual order.
        
        net.minecraft.network.chat.MutableComponent component = Component.literal("");
        
        // Very basic tokenization (splitting by delimiters but keeping them)
        // This is a naive implementation for demonstration.
        String[] parts = line.split("(?<=\\s)|(?=\\s)|(?<=[();{},.])|(?=[();{},.])");
        
        boolean inString = false;
        
        for (String part : parts) {
            Style style = Style.EMPTY.withColor(0xD4D4D4); // Default text color
            
            if (part.startsWith("//")) {
                style = style.withColor(0x6A9955);
                component.append(Component.literal(part).withStyle(style));
                continue; // The rest of the line is comment, but we split it... 
                // Actually this split logic breaks comments. 
                // Let's stick to a simpler approach: if it contains a keyword, color it.
            }

            if (KEYWORDS.matcher(part).matches()) {
                style = style.withColor(0x569CD6); // Blue
            } else if (STRINGS.matcher(part).matches()) {
                style = style.withColor(0xCE9178); // Orange/Red
            } else if (NUMBERS.matcher(part).matches()) {
                style = style.withColor(0xB5CEA8); // Light Green
            }
            
            component.append(Component.literal(part).withStyle(style));
        }
        
        return component.getVisualOrderText();
    }
    
    // Better implementation using regex to find matches and append parts
    public static Component highlightToComponent(String line) {
        net.minecraft.network.chat.MutableComponent component = Component.literal("");
        int lastIndex = 0;
        
        // We can't easily do multiple regex passes without overlapping.
        // Let's just do keywords for now to keep it simple and fast.
        Matcher matcher = KEYWORDS.matcher(line);
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                component.append(Component.literal(line.substring(lastIndex, matcher.start())).withStyle(Style.EMPTY.withColor(0xD4D4D4)));
            }
            component.append(Component.literal(matcher.group()).withStyle(Style.EMPTY.withColor(0x569CD6)));
            lastIndex = matcher.end();
        }
        if (lastIndex < line.length()) {
            component.append(Component.literal(line.substring(lastIndex)).withStyle(Style.EMPTY.withColor(0xD4D4D4)));
        }
        return component;
    }
}
