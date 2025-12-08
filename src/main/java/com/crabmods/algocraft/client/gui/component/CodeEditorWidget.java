package com.crabmods.algocraft.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEditorWidget extends AbstractWidget {
    private final Font font;
    private final List<String> lines = new ArrayList<>();
    private int cursorLine = 0;
    private int cursorCol = 0;
    private int scrollY = 0;
    private int scrollX = 0;
    private int visibleLines;
    private Consumer<String> onValueChanged;
    
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND_COLOR = 0xFF1E1E1E;
    private static final int TEXT_COLOR = 0xFFD4D4D4;
    private static final int KEYWORD_COLOR = 0xFF569CD6; // Blue
    private static final int TYPE_COLOR = 0xFF4EC9B0;    // Teal
    private static final int NUMBER_COLOR = 0xFFB5CEA8;  // Light Green
    private static final int STRING_COLOR = 0xFFCE9178;  // Orange
    private static final int COMMENT_COLOR = 0xFF6A9955; // Green
    
    private static final String[] KEYWORDS = {
        "public", "private", "protected", "class", "interface", "enum", "extends", "implements",
        "void", "int", "boolean", "double", "float", "char", "byte", "short", "long",
        "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return",
        "try", "catch", "finally", "throw", "throws", "new", "this", "super", "static", "final",
        "true", "false", "null", "package", "import"
    };

    public CodeEditorWidget(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
        this.lines.add("");
        this.visibleLines = height / LINE_HEIGHT;
    }

    public void setValue(String text) {
        this.lines.clear();
        if (text == null || text.isEmpty()) {
            this.lines.add("");
        } else {
            this.lines.addAll(Arrays.asList(text.split("\n", -1)));
        }
        this.cursorLine = 0;
        this.cursorCol = 0;
        this.scrollY = 0;
        this.scrollX = 0;
    }

    public String getValue() {
        return String.join("\n", lines);
    }

    public void setResponder(Consumer<String> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Background
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, BACKGROUND_COLOR);
        
        // Border
        guiGraphics.renderOutline(getX(), getY(), width, height, isFocused() ? 0xFFFFFFFF : 0xFF555555);

        // Render Text
        int startLine = scrollY;
        int endLine = Math.min(lines.size(), startLine + visibleLines + 1);
        
        for (int i = startLine; i < endLine; i++) {
            String line = lines.get(i);
            int y = getY() + 2 + (i - startLine) * LINE_HEIGHT;
            
            // Simple Syntax Highlighting
            renderHighlightedLine(guiGraphics, line, getX() + 4 - scrollX, y);
        }
        
        // Render Cursor
        if (isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            if (cursorLine >= startLine && cursorLine < endLine) {
                String lineBeforeCursor = lines.get(cursorLine).substring(0, Math.min(cursorCol, lines.get(cursorLine).length()));
                int cursorX = getX() + 4 - scrollX + font.width(lineBeforeCursor);
                int cursorY = getY() + 2 + (cursorLine - startLine) * LINE_HEIGHT;
                guiGraphics.fill(cursorX, cursorY, cursorX + 1, cursorY + 9, 0xFFFFFFFF);
            }
        }
    }
    
    private void renderHighlightedLine(GuiGraphics guiGraphics, String line, int x, int y) {
        // This is a very basic tokenizer for rendering
        // For a real implementation, we would need a proper lexer
        // Here we just split by spaces and check keywords, which is imperfect but fast
        
        // Actually, splitting by delimiters is better
        // Let's do a simple character loop for now to handle basic tokens
        
        int currentX = x;
        StringBuilder token = new StringBuilder();
        int tokenStart = 0;
        
        for (int i = 0; i <= line.length(); i++) {
            char c = (i < line.length()) ? line.charAt(i) : ' ';
            boolean isDelimiter = " \t(){}[];,.<>+-*/%=&|!".indexOf(c) >= 0;
            
            if (isDelimiter) {
                if (token.length() > 0) {
                    String word = token.toString();
                    int color = TEXT_COLOR;
                    
                    if (isKeyword(word)) color = KEYWORD_COLOR;
                    else if (isType(word)) color = TYPE_COLOR;
                    else if (isNumber(word)) color = NUMBER_COLOR;
                    else if (word.equals("true") || word.equals("false") || word.equals("null")) color = KEYWORD_COLOR;
                    
                    guiGraphics.drawString(font, word, currentX, y, color, false);
                    currentX += font.width(word);
                    token.setLength(0);
                }
                
                if (i < line.length()) {
                    // Draw the delimiter
                    guiGraphics.drawString(font, String.valueOf(c), currentX, y, TEXT_COLOR, false);
                    currentX += font.width(String.valueOf(c));
                }
            } else {
                token.append(c);
            }
        }
    }
    
    private boolean isKeyword(String word) {
        for (String k : KEYWORDS) if (k.equals(word)) return true;
        return false;
    }
    
    private boolean isType(String word) {
        return Character.isUpperCase(word.charAt(0)); // Simple heuristic for classes
    }
    
    private boolean isNumber(String word) {
        try {
            Double.parseDouble(word);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused()) return false;
        
        String line = lines.get(cursorLine);
        String newLine = line.substring(0, cursorCol) + codePoint + line.substring(cursorCol);
        lines.set(cursorLine, newLine);
        cursorCol++;
        notifyChange();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        if (Screen.isSelectAll(keyCode)) {
            // Select all (not implemented fully, just ignore)
            return true;
        }
        
        if (Screen.isCopy(keyCode) || Screen.isPaste(keyCode) || Screen.isCut(keyCode)) {
            // Clipboard (simplified: paste at cursor)
            if (Screen.isPaste(keyCode)) {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clipboard != null) {
                    insertText(clipboard);
                }
                return true;
            }
            return false;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE:
                if (cursorCol > 0) {
                    String line = lines.get(cursorLine);
                    lines.set(cursorLine, line.substring(0, cursorCol - 1) + line.substring(cursorCol));
                    cursorCol--;
                } else if (cursorLine > 0) {
                    String currentLine = lines.get(cursorLine);
                    String prevLine = lines.get(cursorLine - 1);
                    lines.set(cursorLine - 1, prevLine + currentLine);
                    lines.remove(cursorLine);
                    cursorLine--;
                    cursorCol = prevLine.length();
                }
                notifyChange();
                return true;
            case GLFW.GLFW_KEY_ENTER:
                String line = lines.get(cursorLine);
                String before = line.substring(0, cursorCol);
                String after = line.substring(cursorCol);
                
                // Auto-indent
                int indent = 0;
                while (indent < before.length() && before.charAt(indent) == ' ') indent++;
                if (before.trim().endsWith("{")) indent += 4;
                
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < indent; i++) sb.append(' ');
                
                lines.set(cursorLine, before);
                lines.add(cursorLine + 1, sb.toString() + after);
                cursorLine++;
                cursorCol = indent;
                notifyChange();
                return true;
            case GLFW.GLFW_KEY_LEFT:
                if (cursorCol > 0) cursorCol--;
                else if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = lines.get(cursorLine).length();
                }
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                if (cursorCol < lines.get(cursorLine).length()) cursorCol++;
                else if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = 0;
                }
                return true;
            case GLFW.GLFW_KEY_UP:
                if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                return true;
            case GLFW.GLFW_KEY_DOWN:
                if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                return true;
            case GLFW.GLFW_KEY_TAB:
                insertText("    ");
                return true;
        }
        
        return false;
    }
    
    private void insertText(String text) {
        String[] parts = text.split("\n", -1);
        String line = lines.get(cursorLine);
        String before = line.substring(0, cursorCol);
        String after = line.substring(cursorCol);
        
        if (parts.length == 1) {
            lines.set(cursorLine, before + parts[0] + after);
            cursorCol += parts[0].length();
        } else {
            lines.set(cursorLine, before + parts[0]);
            for (int i = 1; i < parts.length - 1; i++) {
                lines.add(cursorLine + i, parts[i]);
            }
            lines.add(cursorLine + parts.length - 1, parts[parts.length - 1] + after);
            cursorLine += parts.length - 1;
            cursorCol = parts[parts.length - 1].length();
        }
        notifyChange();
    }
    
    private void notifyChange() {
        if (onValueChanged != null) {
            onValueChanged.accept(getValue());
        }
        // Adjust scroll
        if (cursorLine < scrollY) scrollY = cursorLine;
        if (cursorLine >= scrollY + visibleLines) scrollY = cursorLine - visibleLines + 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            setFocused(true);
            // Calculate cursor position from mouse
            int relY = (int) (mouseY - getY() - 2);
            int lineIndex = scrollY + relY / LINE_HEIGHT;
            
            if (lineIndex >= 0 && lineIndex < lines.size()) {
                cursorLine = lineIndex;
                String line = lines.get(lineIndex);
                int relX = (int) (mouseX - getX() - 4 + scrollX);
                
                // Estimate column (simple)
                int col = 0;
                int w = 0;
                while (col < line.length()) {
                    int charW = font.width(String.valueOf(line.charAt(col)));
                    if (w + charW / 2 > relX) break;
                    w += charW;
                    col++;
                }
                cursorCol = col;
            }
            return true;
        } else {
            setFocused(false);
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // No-op
    }
}
