package com.crabmods.algocraft.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import com.crabmods.algocraft.client.gui.modern.IdeTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class CodeEditorWidget extends AbstractWidget {
    private final Font font;
    private final List<String> lines = new ArrayList<>();
    private int cursorLine = 0;
    private int cursorCol = 0;
    private long caretBlinkStartedAt = Util.getMillis();
    private int scrollY = 0;
    private int scrollX = 0;
    private int visibleLines;
    private Consumer<String> onValueChanged;
    private final Deque<EditorState> undoStack = new ArrayDeque<>();
    private final Deque<EditorState> redoStack = new ArrayDeque<>();
    
    // Selection support
    private int selectionStartLine = -1;
    private int selectionStartCol = -1;
    private int selectionEndLine = -1;
    private int selectionEndCol = -1;
    private boolean hasSelection = false;
    
    private static final int LINE_HEIGHT = 10;
    private static final int LINE_NUMBER_WIDTH = 25;
    private static final int MAX_UNDO_STATES = 100;
    private static final int BACKGROUND_COLOR = IdeTheme.BACKGROUND;
    private static final int GUTTER_COLOR = 0xFF10151C;
    private static final int CURRENT_LINE_COLOR = 0xFF161D27;
    private static final int LINE_NUMBER_COLOR = IdeTheme.TEXT_DIM;
    private static final int CURRENT_LINE_NUMBER_COLOR = IdeTheme.TEXT_MUTED;
    private static final int TEXT_COLOR = 0xFFE6EDF3;
    private static final int KEYWORD_COLOR = 0xFFFF7B72; // Coral
    private static final int TYPE_COLOR = 0xFFFFA657;    // Amber
    private static final int NUMBER_COLOR = 0xFF79C0FF;  // Sky
    private static final int STRING_COLOR = 0xFFA5D6FF;  // Light blue
    private static final int COMMENT_COLOR = 0xFF8B949E; // Grey
    private static final int SELECTION_COLOR = 0xFF1F3A5F; // Selection background
    
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

    public void setBounds(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.width = width;
        this.height = height;
        this.visibleLines = Math.max(1, height / LINE_HEIGHT);
        ensureCursorVisible();
    }

    public void setValue(String text) {
        this.lines.clear();
        String normalized = CodeEditorTextNormalizer.normalizeLineEndings(text);
        if (normalized.isEmpty()) {
            this.lines.add("");
        } else {
            this.lines.addAll(Arrays.asList(normalized.split("\n", -1)));
        }
        this.cursorLine = 0;
        this.cursorCol = 0;
        this.scrollY = 0;
        this.scrollX = 0;
        this.undoStack.clear();
        this.redoStack.clear();
        clearSelection();
        resetCaretBlink();
    }

    public String getValue() {
        return String.join("\n", lines);
    }

    public void setResponder(Consumer<String> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Background, gutter and border
        IdeTheme.frame(guiGraphics, getX(), getY(), width, height, BACKGROUND_COLOR, isFocused() ? IdeTheme.INFO : IdeTheme.BORDER);
        guiGraphics.fill(getX() + 1, getY() + 1, getX() + LINE_NUMBER_WIDTH, getY() + height - 1, GUTTER_COLOR);

        // Render Text
        int startLine = scrollY;
        int endLine = Math.min(lines.size(), startLine + visibleLines + 1);

        // Line numbers: clipped to the gutter.
        guiGraphics.enableScissor(getX() + 1, getY() + 1, getX() + LINE_NUMBER_WIDTH, getY() + height - 1);
        for (int i = startLine; i < endLine; i++) {
            int y = getY() + 2 + (i - startLine) * LINE_HEIGHT;
            String lineNumStr = String.valueOf(i + 1);
            int numberColor = isFocused() && i == cursorLine ? CURRENT_LINE_NUMBER_COLOR : LINE_NUMBER_COLOR;
            guiGraphics.drawString(font, lineNumStr, getX() + LINE_NUMBER_WIDTH - 4 - font.width(lineNumStr), y, numberColor, false);
        }
        guiGraphics.disableScissor();

        // Code: clipped to the text area so horizontal scrolling never draws over the gutter.
        guiGraphics.enableScissor(getX() + LINE_NUMBER_WIDTH + 1, getY() + 1, getX() + width - 1, getY() + height - 1);

        for (int i = startLine; i < endLine; i++) {
            String line = lines.get(i);
            int y = getY() + 2 + (i - startLine) * LINE_HEIGHT;

            if (isFocused() && i == cursorLine && !hasSelection) {
                guiGraphics.fill(getX() + LINE_NUMBER_WIDTH + 1, y - 1, getX() + width - 1, y + LINE_HEIGHT - 1, CURRENT_LINE_COLOR);
            }

            // Draw selection background
            if (hasSelection) {
                renderSelectionBackground(guiGraphics, i, line, getX() + LINE_NUMBER_WIDTH + 4 - scrollX, y);
            }

            // Simple Syntax Highlighting
            renderHighlightedLine(guiGraphics, line, getX() + LINE_NUMBER_WIDTH + 4 - scrollX, y);
        }
        
        // Render Cursor
        if (isFocused() && (Util.getMillis() - caretBlinkStartedAt) % 1000 < 500) {
            if (cursorLine >= startLine && cursorLine < endLine) {
                String lineBeforeCursor = lines.get(cursorLine).substring(0, Math.min(cursorCol, lines.get(cursorLine).length()));
                int cursorX = getX() + LINE_NUMBER_WIDTH + 4 - scrollX + font.width(lineBeforeCursor);
                int cursorY = getY() + 2 + (cursorLine - startLine) * LINE_HEIGHT;
                guiGraphics.fill(cursorX, cursorY, cursorX + 1, cursorY + 9, IdeTheme.INFO);
            }
        }
        
        // Render Scrollbar
        if (lines.size() > visibleLines) {
            int scrollbarHeight = (int) ((float) visibleLines / lines.size() * height);
            if (scrollbarHeight < 10) scrollbarHeight = 10;
            int scrollbarY = getY() + (int) ((float) scrollY / (lines.size() - visibleLines) * (height - scrollbarHeight));
            
            int scrollbarX = getX() + width - 6;
            guiGraphics.fill(scrollbarX + 1, scrollbarY + 1, scrollbarX + 4, scrollbarY + scrollbarHeight - 1, IdeTheme.BORDER_STRONG);
        }
        
        guiGraphics.disableScissor();
    }
    
    private void renderSelectionBackground(GuiGraphics guiGraphics, int lineIndex, String line, int x, int y) {
        if (!hasSelection) return;
        
        int startLine = Math.min(selectionStartLine, selectionEndLine);
        int endLine = Math.max(selectionStartLine, selectionEndLine);
        int startCol, endCol;
        
        if (selectionStartLine < selectionEndLine || 
            (selectionStartLine == selectionEndLine && selectionStartCol <= selectionEndCol)) {
            startCol = selectionStartCol;
            endCol = selectionEndCol;
        } else {
            startCol = selectionEndCol;
            endCol = selectionStartCol;
        }
        
        if (lineIndex < startLine || lineIndex > endLine) return;
        
        int selStart = 0;
        int selEnd = line.length();
        
        if (lineIndex == startLine) {
            selStart = Math.min(startCol, line.length());
        }
        if (lineIndex == endLine) {
            selEnd = Math.min(endCol, line.length());
        }
        
        if (selStart < selEnd) {
            int xStart = x + font.width(line.substring(0, selStart));
            int xEnd = x + font.width(line.substring(0, selEnd));
            guiGraphics.fill(xStart, y - 1, xEnd, y + LINE_HEIGHT - 1, SELECTION_COLOR);
        } else if (lineIndex > startLine && lineIndex < endLine) {
            // Full line selection for middle lines
            int lineWidth = Math.max(font.width(line), 5);
            guiGraphics.fill(x, y - 1, x + lineWidth, y + LINE_HEIGHT - 1, SELECTION_COLOR);
        }
    }
    
    private void renderHighlightedLine(GuiGraphics guiGraphics, String line, int x, int y) {
        int currentX = x;
        int i = 0;
        while (i < line.length()) {
            // Check for comment
            if (i + 1 < line.length() && line.charAt(i) == '/' && line.charAt(i + 1) == '/') {
                String comment = line.substring(i);
                guiGraphics.drawString(font, comment, currentX, y, COMMENT_COLOR, false);
                return; // Rest of line is comment
            }

            // Check for string
            if (line.charAt(i) == '"') {
                int end = line.indexOf('"', i + 1);
                while (end != -1 && line.charAt(end - 1) == '\\') { // Handle escaped quotes
                    end = line.indexOf('"', end + 1);
                }
                if (end == -1) end = line.length(); // Unclosed string
                else end++; // Include closing quote
                
                String stringLiteral = line.substring(i, end);
                guiGraphics.drawString(font, stringLiteral, currentX, y, STRING_COLOR, false);
                currentX += font.width(stringLiteral);
                i = end;
                continue;
            }

            char c = line.charAt(i);
            if (Character.isWhitespace(c)) {
                guiGraphics.drawString(font, String.valueOf(c), currentX, y, TEXT_COLOR, false);
                currentX += font.width(String.valueOf(c));
                i++;
                continue;
            }

            if (isDelimiter(c)) {
                guiGraphics.drawString(font, String.valueOf(c), currentX, y, TEXT_COLOR, false);
                currentX += font.width(String.valueOf(c));
                i++;
                continue;
            }

            // Identifier or Number
            int start = i;
            while (i < line.length() && !isDelimiter(line.charAt(i)) && !Character.isWhitespace(line.charAt(i)) && line.charAt(i) != '"') {
                i++;
            }
            String word = line.substring(start, i);
            int color = TEXT_COLOR;
            if (isKeyword(word)) color = KEYWORD_COLOR;
            else if (isType(word)) color = TYPE_COLOR;
            else if (isNumber(word)) color = NUMBER_COLOR;
            else if (word.equals("true") || word.equals("false") || word.equals("null")) color = KEYWORD_COLOR;
            
            guiGraphics.drawString(font, word, currentX, y, color, false);
            currentX += font.width(word);
        }
    }

    private boolean isDelimiter(char c) {
        return "(){}[];,.<>+-*/%=&|!".indexOf(c) >= 0;
    }
    
    private boolean isKeyword(String word) {
        for (String k : KEYWORDS) if (k.equals(word)) return true;
        return false;
    }
    
    private boolean isType(String word) {
        return Character.isUpperCase(word.charAt(0)); // Simple heuristic for classes
    }
    
    private boolean isNumber(String word) {
        if (word.isEmpty()) return false;
        if (word.startsWith("0x") || word.startsWith("0X")) {
            try {
                Long.parseLong(word.substring(2), 16);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        char last = word.charAt(word.length() - 1);
        String check = word;
        if (last == 'f' || last == 'F' || last == 'd' || last == 'D' || last == 'l' || last == 'L') {
            check = word.substring(0, word.length() - 1);
        }
        try {
            Double.parseDouble(check);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused()) return false;
        if (Screen.hasControlDown()
                || Screen.hasAltDown()
                || (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_ALT)) != 0) {
            return false;
        }
        
        pushUndoState();

        // Delete selection first if any
        if (hasSelection) {
            deleteSelection();
        }
        
        String line = lines.get(cursorLine);
        String newLine = line.substring(0, cursorCol) + codePoint + line.substring(cursorCol);
        lines.set(cursorLine, newLine);
        cursorCol++;
        resetCaretBlink();
        notifyChange();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = handleKeyPressed(keyCode, scanCode, modifiers);
        if (handled) resetCaretBlink();
        return handled;
    }

    private boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;
        
        boolean shiftDown = Screen.hasShiftDown() || (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrlDown = Screen.hasControlDown()
                || (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        boolean altDown = Screen.hasAltDown() || (modifiers & GLFW.GLFW_MOD_ALT) != 0;

        if (ctrlDown && !altDown && keyCode == GLFW.GLFW_KEY_Z) {
            if (shiftDown) {
                redo();
            } else {
                undo();
            }
            return true;
        }

        if (ctrlDown && !altDown && keyCode == GLFW.GLFW_KEY_Y) {
            redo();
            return true;
        }

        if (ctrlDown && !altDown && keyCode == GLFW.GLFW_KEY_A) {
            selectAll();
            return true;
        }
        
        if (ctrlDown && !altDown && keyCode == GLFW.GLFW_KEY_C) {
            copySelection();
            return true;
        }
        
        if (ctrlDown && !altDown && keyCode == GLFW.GLFW_KEY_X) {
            pushUndoState();
            cutSelection();
            return true;
        }
        
        if (ctrlDown && !altDown && keyCode == GLFW.GLFW_KEY_V) {
            pushUndoState();
            deleteSelection();
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard != null) {
                insertText(clipboard);
            }
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_DELETE:
                pushUndoState();
                if (hasSelection) {
                    deleteSelection();
                } else if (cursorCol < lines.get(cursorLine).length()) {
                    String line = lines.get(cursorLine);
                    lines.set(cursorLine, line.substring(0, cursorCol) + line.substring(cursorCol + 1));
                } else if (cursorLine < lines.size() - 1) {
                    String currentLine = lines.get(cursorLine);
                    String nextLine = lines.get(cursorLine + 1);
                    lines.set(cursorLine, currentLine + nextLine);
                    lines.remove(cursorLine + 1);
                }
                notifyChange();
                return true;
            case GLFW.GLFW_KEY_HOME:
                if (shiftDown) {
                    startOrExtendSelection();
                } else {
                    clearSelection();
                }
                if (ctrlDown) {
                    cursorLine = 0;
                    cursorCol = 0;
                } else {
                    int firstTextCol = firstNonWhitespaceColumn(lines.get(cursorLine));
                    cursorCol = cursorCol == firstTextCol ? 0 : firstTextCol;
                }
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_END:
                if (shiftDown) {
                    startOrExtendSelection();
                } else {
                    clearSelection();
                }
                if (ctrlDown) {
                    cursorLine = lines.size() - 1;
                    cursorCol = lines.get(cursorLine).length();
                } else {
                    cursorCol = lines.get(cursorLine).length();
                }
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_PAGE_UP:
                if (shiftDown) startOrExtendSelection(); else clearSelection();
                cursorLine = Math.max(0, cursorLine - visibleLines);
                cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_PAGE_DOWN:
                if (shiftDown) startOrExtendSelection(); else clearSelection();
                cursorLine = Math.min(lines.size() - 1, cursorLine + visibleLines);
                cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_BACKSPACE:
                pushUndoState();
                if (hasSelection) {
                    deleteSelection();
                } else if (cursorCol > 0) {
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
                pushUndoState();
                if (hasSelection) {
                    deleteSelection();
                }
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
                if (shiftDown) startOrExtendSelection(); else clearSelection();
                moveCursorHorizontal(-1, ctrlDown);
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                if (shiftDown) startOrExtendSelection(); else clearSelection();
                moveCursorHorizontal(1, ctrlDown);
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_UP:
                if (shiftDown) startOrExtendSelection(); else clearSelection();
                if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_DOWN:
                if (shiftDown) startOrExtendSelection(); else clearSelection();
                if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                if (shiftDown) updateSelectionEnd();
                ensureCursorVisible();
                return true;
            case GLFW.GLFW_KEY_TAB:
                pushUndoState();
                if (shiftDown) {
                    unindentSelectedLines();
                } else if (hasSelection) {
                    indentSelectedLines();
                } else {
                    insertText("    ");
                }
                return true;
        }
        
        return false;
    }

    private void pushUndoState() {
        EditorState state = captureState();
        if (!undoStack.isEmpty() && undoStack.peek().equals(state)) {
            return;
        }
        undoStack.push(state);
        while (undoStack.size() > MAX_UNDO_STATES) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private EditorState captureState() {
        return new EditorState(new ArrayList<>(lines), cursorLine, cursorCol, scrollY, scrollX);
    }

    private void restoreState(EditorState state) {
        lines.clear();
        lines.addAll(state.lines());
        cursorLine = Mth.clamp(state.cursorLine(), 0, lines.size() - 1);
        cursorCol = Mth.clamp(state.cursorCol(), 0, lines.get(cursorLine).length());
        scrollY = Math.max(0, state.scrollY());
        scrollX = Math.max(0, state.scrollX());
        clearSelection();
        ensureCursorVisible();
        notifyValueChanged();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        redoStack.push(captureState());
        restoreState(undoStack.pop());
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        undoStack.push(captureState());
        restoreState(redoStack.pop());
    }

    private void moveCursorHorizontal(int direction, boolean byWord) {
        if (!byWord) {
            if (direction < 0) {
                if (cursorCol > 0) cursorCol--;
                else if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = lines.get(cursorLine).length();
                }
            } else {
                if (cursorCol < lines.get(cursorLine).length()) cursorCol++;
                else if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = 0;
                }
            }
            return;
        }

        if (direction < 0) {
            moveToPreviousWord();
        } else {
            moveToNextWord();
        }
    }

    private void moveToPreviousWord() {
        if (cursorCol == 0) {
            if (cursorLine > 0) {
                cursorLine--;
                cursorCol = lines.get(cursorLine).length();
            }
            return;
        }

        String line = lines.get(cursorLine);
        int col = cursorCol;
        while (col > 0 && Character.isWhitespace(line.charAt(col - 1))) {
            col--;
        }
        if (col > 0 && isWordPart(line.charAt(col - 1))) {
            while (col > 0 && isWordPart(line.charAt(col - 1))) {
                col--;
            }
        } else if (col > 0) {
            col--;
        }
        cursorCol = col;
    }

    private void moveToNextWord() {
        String line = lines.get(cursorLine);
        if (cursorCol >= line.length()) {
            if (cursorLine < lines.size() - 1) {
                cursorLine++;
                cursorCol = 0;
            }
            return;
        }

        int col = cursorCol;
        if (isWordPart(line.charAt(col))) {
            while (col < line.length() && isWordPart(line.charAt(col))) {
                col++;
            }
        } else {
            col++;
        }
        while (col < line.length() && Character.isWhitespace(line.charAt(col))) {
            col++;
        }
        cursorCol = col;
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int firstNonWhitespaceColumn(String line) {
        int col = 0;
        while (col < line.length() && Character.isWhitespace(line.charAt(col))) {
            col++;
        }
        return col;
    }

    private void indentSelectedLines() {
        int[] range = selectedLineRange();
        for (int i = range[0]; i <= range[1]; i++) {
            lines.set(i, "    " + lines.get(i));
        }
        adjustSelectionColumnsForIndent(4);
        notifyChange();
    }

    private void unindentSelectedLines() {
        int[] range = selectedLineRange();
        int cursorRemoved = 0;
        int selectionStartRemoved = 0;
        int selectionEndRemoved = 0;
        for (int i = range[0]; i <= range[1]; i++) {
            String line = lines.get(i);
            int removed = leadingIndentToRemove(line);
            if (removed > 0) {
                lines.set(i, line.substring(removed));
            }
            if (i == cursorLine) cursorRemoved = removed;
            if (i == selectionStartLine) selectionStartRemoved = removed;
            if (i == selectionEndLine) selectionEndRemoved = removed;
        }
        cursorCol = Math.max(0, cursorCol - cursorRemoved);
        if (hasSelection) {
            selectionStartCol = Math.max(0, selectionStartCol - selectionStartRemoved);
            selectionEndCol = Math.max(0, selectionEndCol - selectionEndRemoved);
        }
        notifyChange();
    }

    private int[] selectedLineRange() {
        if (!hasSelection) {
            return new int[] { cursorLine, cursorLine };
        }
        return new int[] {
            Math.min(selectionStartLine, selectionEndLine),
            Math.max(selectionStartLine, selectionEndLine)
        };
    }

    private void adjustSelectionColumnsForIndent(int amount) {
        cursorCol += amount;
        if (hasSelection) {
            selectionStartCol += amount;
            selectionEndCol += amount;
        }
    }

    private static int leadingIndentToRemove(String line) {
        if (line.startsWith("    ")) {
            return 4;
        }
        if (line.startsWith("\t")) {
            return 1;
        }
        int spaces = 0;
        while (spaces < line.length() && spaces < 4 && line.charAt(spaces) == ' ') {
            spaces++;
        }
        return spaces;
    }
    
    private void insertText(String text) {
        String[] parts = CodeEditorTextNormalizer.normalizeLineEndings(text).split("\n", -1);
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
        ensureCursorVisible();
        notifyValueChanged();
    }

    private void notifyValueChanged() {
        if (onValueChanged != null) {
            onValueChanged.accept(getValue());
        }
    }

    private void ensureCursorVisible() {
        this.visibleLines = Math.max(1, height / LINE_HEIGHT);
        if (cursorLine < scrollY) scrollY = cursorLine;
        if (cursorLine >= scrollY + visibleLines) scrollY = cursorLine - visibleLines + 1;
        scrollY = Mth.clamp(scrollY, 0, Math.max(0, lines.size() - visibleLines));

        int textAreaWidth = CodeEditorViewport.textAreaWidth(width, LINE_NUMBER_WIDTH);
        String lineBeforeCursor = lines.get(cursorLine).substring(0, Math.min(cursorCol, lines.get(cursorLine).length()));
        int cursorPixel = textWidth(lineBeforeCursor);
        if (cursorPixel < scrollX) {
            scrollX = cursorPixel;
        } else if (cursorPixel > scrollX + textAreaWidth - CodeEditorViewport.TEXT_AREA_RIGHT_PADDING) {
            scrollX = cursorPixel - textAreaWidth + CodeEditorViewport.TEXT_AREA_RIGHT_PADDING;
        }

        scrollX = CodeEditorViewport.clampHorizontalScroll(scrollX, width, LINE_NUMBER_WIDTH, maxLineWidth());
    }

    private int textWidth(String text) {
        return font != null ? font.width(text) : text.length() * 6;
    }

    private int maxLineWidth() {
        int maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(maxLineWidth, textWidth(line));
        }
        return maxLineWidth;
    }

    // Selection helper methods
    private void clearSelection() {
        hasSelection = false;
        selectionStartLine = -1;
        selectionStartCol = -1;
        selectionEndLine = -1;
        selectionEndCol = -1;
    }
    
    private void startOrExtendSelection() {
        if (!hasSelection) {
            selectionStartLine = cursorLine;
            selectionStartCol = cursorCol;
            selectionEndLine = cursorLine;
            selectionEndCol = cursorCol;
            hasSelection = true;
        }
    }
    
    private void updateSelectionEnd() {
        selectionEndLine = cursorLine;
        selectionEndCol = cursorCol;
    }
    
    private void selectAll() {
        selectionStartLine = 0;
        selectionStartCol = 0;
        selectionEndLine = lines.size() - 1;
        selectionEndCol = lines.get(lines.size() - 1).length();
        cursorLine = selectionEndLine;
        cursorCol = selectionEndCol;
        hasSelection = true;
    }
    
    private String getSelectedText() {
        if (!hasSelection) return "";
        
        int startLine = selectionStartLine;
        int startCol = selectionStartCol;
        int endLine = selectionEndLine;
        int endCol = selectionEndCol;
        
        // Normalize selection direction
        if (startLine > endLine || (startLine == endLine && startCol > endCol)) {
            int tempLine = startLine; startLine = endLine; endLine = tempLine;
            int tempCol = startCol; startCol = endCol; endCol = tempCol;
        }
        
        if (startLine == endLine) {
            String line = lines.get(startLine);
            return line.substring(Math.min(startCol, line.length()), Math.min(endCol, line.length()));
        }
        
        StringBuilder sb = new StringBuilder();
        // First line
        String firstLine = lines.get(startLine);
        sb.append(firstLine.substring(Math.min(startCol, firstLine.length())));
        
        // Middle lines
        for (int i = startLine + 1; i < endLine; i++) {
            sb.append("\n").append(lines.get(i));
        }
        
        // Last line
        String lastLine = lines.get(endLine);
        sb.append("\n").append(lastLine.substring(0, Math.min(endCol, lastLine.length())));
        
        return sb.toString();
    }
    
    private void copySelection() {
        if (hasSelection) {
            String text = getSelectedText();
            if (!text.isEmpty()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
            }
        }
    }
    
    private void cutSelection() {
        if (hasSelection) {
            copySelection();
            deleteSelection();
        }
    }
    
    private void deleteSelection() {
        if (!hasSelection) return;
        
        int startLine = selectionStartLine;
        int startCol = selectionStartCol;
        int endLine = selectionEndLine;
        int endCol = selectionEndCol;
        
        // Normalize selection direction
        if (startLine > endLine || (startLine == endLine && startCol > endCol)) {
            int tempLine = startLine; startLine = endLine; endLine = tempLine;
            int tempCol = startCol; startCol = endCol; endCol = tempCol;
        }
        
        if (startLine == endLine) {
            String line = lines.get(startLine);
            startCol = Math.min(startCol, line.length());
            endCol = Math.min(endCol, line.length());
            lines.set(startLine, line.substring(0, startCol) + line.substring(endCol));
        } else {
            String firstLine = lines.get(startLine);
            String lastLine = lines.get(endLine);
            startCol = Math.min(startCol, firstLine.length());
            endCol = Math.min(endCol, lastLine.length());
            
            lines.set(startLine, firstLine.substring(0, startCol) + lastLine.substring(endCol));
            for (int i = endLine; i > startLine; i--) {
                lines.remove(i);
            }
        }
        
        cursorLine = startLine;
        cursorCol = startCol;
        clearSelection();
        notifyChange();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            if (Screen.hasShiftDown()) {
                this.scrollX = CodeEditorViewport.clampHorizontalScroll(
                        this.scrollX - (int) (scrollY * 10),
                        width,
                        LINE_NUMBER_WIDTH,
                        maxLineWidth()
                );
            } else {
                this.scrollY -= (int) scrollY;
                if (this.scrollY < 0) this.scrollY = 0;
                if (this.scrollY > lines.size() - visibleLines) {
                    this.scrollY = Math.max(0, lines.size() - visibleLines);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            setFocused(true);
            clearSelection();
            // Calculate cursor position from mouse (fixed: account for LINE_NUMBER_WIDTH)
            int relY = (int) (mouseY - getY() - 2);
            int lineIndex = scrollY + relY / LINE_HEIGHT;
            
            if (lineIndex >= 0 && lineIndex < lines.size()) {
                cursorLine = lineIndex;
                String line = lines.get(lineIndex);
                int relX = (int) (mouseX - getX() - LINE_NUMBER_WIDTH - 4 + scrollX);
                
                // Estimate column (simple)
                int col = 0;
                int w = 0;
                while (col < line.length()) {
                    int charW = textWidth(String.valueOf(line.charAt(col)));
                    if (w + charW / 2 > relX) break;
                    w += charW;
                    col++;
                }
                cursorCol = Math.max(0, col);
            }
            resetCaretBlink();
            return true;
        } else {
            setFocused(false);
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !isFocused()) resetCaretBlink();
        super.setFocused(focused);
    }

    private void resetCaretBlink() {
        caretBlinkStartedAt = Util.getMillis();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // No-op
    }

    private record EditorState(List<String> lines, int cursorLine, int cursorCol, int scrollY, int scrollX) {}
}
