package com.crabmods.algocraft.client.gui.modern;

import com.crabmods.algocraft.logic.ProblemManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ImportProblemScreen extends Screen {
    private final Screen parent;
    private EditBox inputField;
    private EditBox nameField;
    private Component status = Component.empty();
    private int importType = 0; // 0: URL Repository, 1: Local File
    private Button typeButton;

    public ImportProblemScreen(Screen parent) {
        super(Component.translatable("algocraft.gui.import_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Type Button
        this.typeButton = Button.builder(getTypeText(), button -> {
            importType = (importType + 1) % 2;
            button.setMessage(getTypeText());
            updateVisibility();
        }).bounds(centerX - 100, centerY - 60, 200, 20).build();
        this.addRenderableWidget(this.typeButton);

        // Name Field (for Repository Name)
        this.nameField = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.translatable("algocraft.gui.import.name"));
        this.nameField.setHint(Component.translatable("algocraft.gui.import.name_hint"));
        this.addRenderableWidget(this.nameField);

        // Input Field (URL or Path)
        this.inputField = new EditBox(this.font, centerX - 100, centerY, 200, 20, Component.translatable("algocraft.gui.import.input"));
        this.addRenderableWidget(this.inputField);

        // Import Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.import.do_import"), button -> {
            doImport();
        }).bounds(centerX - 100, centerY + 30, 200, 20).build());

        // Back Button
        this.addRenderableWidget(Button.builder(Component.translatable("algocraft.gui.import.back"), button -> {
            this.onClose();
        }).bounds(centerX - 100, centerY + 60, 200, 20).build());
        
        updateVisibility();
    }
    
    private Component getTypeText() {
        return switch (importType) {
            case 0 -> Component.translatable("algocraft.gui.import.mode.remote");
            case 1 -> Component.translatable("algocraft.gui.import.mode.local");
            default -> Component.translatable("algocraft.gui.import.mode.unknown");
        };
    }
    
    private void updateVisibility() {
        if (importType == 0) {
            nameField.visible = true;
            inputField.setHint(Component.translatable("algocraft.gui.import.url_hint"));
        } else {
            nameField.visible = false;
            inputField.setHint(Component.translatable("algocraft.gui.import.file_hint"));
        }
    }

    private void doImport() {
        String input = inputField.getValue();
        if (input.isEmpty()) {
            status = Component.translatable("algocraft.gui.import.status.empty_input");
            return;
        }

        if (importType == 0) {
            // Remote Repository
            String name = nameField.getValue();
            if (name.isEmpty()) {
                status = Component.translatable("algocraft.gui.import.status.empty_name");
                return;
            }
            
            status = Component.translatable("algocraft.gui.import.status.downloading");
            ProblemManager.downloadAndUpdateRepository(name, input)
                .thenRun(() -> {
                    status = Component.translatable("algocraft.gui.import.status.success");
                })
                .exceptionally(e -> {
                    status = Component.translatable("algocraft.gui.import.status.error", e.getMessage());
                    return null;
                });
        } else {
            // Local File
            File file = new File(input);
            if (!file.exists()) {
                status = Component.translatable("algocraft.gui.import.status.file_not_found");
                return;
            }
            
            try {
                File userDir = new File(Minecraft.getInstance().gameDirectory, "algorithm_challenges/user");
                if (!userDir.exists()) userDir.mkdirs();
                
                Files.copy(file.toPath(), userDir.toPath().resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
                ProblemManager.refreshAll();
                status = Component.translatable("algocraft.gui.import.status.success");
            } catch (Exception e) {
                status = Component.translatable("algocraft.gui.import.status.error", e.getMessage());
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, status, this.width / 2, this.height / 2 + 90, 0xFFFF00);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
