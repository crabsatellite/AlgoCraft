package com.crabmods.algocraft.client.gui.modern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    // Java Keywords
    private static final Pattern KEYWORDS = Pattern.compile("\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null)\\b");
    private static final Pattern STRINGS = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern COMMENTS = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/");
    private static final Pattern NUMBERS = Pattern.compile("\\b\\d+\\b");
    private static final Pattern ANNOTATIONS = Pattern.compile("@\\w+");

    public static List<FormattedCharSequence> highlight(String text, net.minecraft.client.gui.Font font) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        // Split by logical lines for processing, but we need to handle wrapping if the editor wraps.
        // However, for code editors, we usually don't want wrapping or we handle it carefully.
        // Here we assume the text is split by newlines.
        
        String[] rawLines = text.split("\n", -1);
        for (String line : rawLines) {
            lines.add(highlightLine(line).getVisualOrderText());
        }
        return lines;
    }

    public static Component highlightLine(String line) {
        net.minecraft.network.chat.MutableComponent component = Component.literal("");
        
        // A simple tokenizer would be better, but for now we use a multi-pass regex approach 
        // or a simple split. Since we can't easily overlap styles in a single Component builder 
        // without a proper parser, we will do a simplified approach:
        // Split by delimiters and colorize tokens.
        
        // Regex to split but keep delimiters: ((?<=\b)|(?=\b)) is too simple.
        // Let's try to match tokens and append them.
        
        int lastIndex = 0;
        // We will just match all interesting things and fill the gaps with plain text.
        // To do this correctly with multiple patterns, we need a combined pattern.
        
        Pattern COMBINED = Pattern.compile(
            "(?<COMMENT>//.*)|" +
            "(?<STRING>\"([^\"\\\\]|\\\\.)*\")|" +
            "(?<KEYWORD>\\b(public|private|protected|class|void|int|boolean|if|else|for|while|return|new|static|final|import|package|try|catch|throw|throws)\\b)|" +
            "(?<NUMBER>\\b\\d+\\b)|" +
            "(?<ANNOTATION>@\\w+)"
        );
        
        Matcher matcher = COMBINED.matcher(line);
        
        while (matcher.find()) {
            // Append plain text before match
            if (matcher.start() > lastIndex) {
                component.append(Component.literal(line.substring(lastIndex, matcher.start())).withStyle(Style.EMPTY.withColor(0xD4D4D4)));
            }
            
            String match = matcher.group();
            Style style = Style.EMPTY.withColor(0xD4D4D4);
            
            if (matcher.group("COMMENT") != null) {
                style = style.withColor(0x6A9955); // Green
            } else if (matcher.group("STRING") != null) {
                style = style.withColor(0xCE9178); // Orange
            } else if (matcher.group("KEYWORD") != null) {
                style = style.withColor(0x569CD6); // Blue
            } else if (matcher.group("NUMBER") != null) {
                style = style.withColor(0xB5CEA8); // Light Green
            } else if (matcher.group("ANNOTATION") != null) {
                style = style.withColor(0xDCDCAA); // Yellowish
            }
            
            component.append(Component.literal(match).withStyle(style));
            lastIndex = matcher.end();
        }
        
        // Append remaining text
        if (lastIndex < line.length()) {
            component.append(Component.literal(line.substring(lastIndex)).withStyle(Style.EMPTY.withColor(0xD4D4D4)));
        }
        
        return component;
    }
}
