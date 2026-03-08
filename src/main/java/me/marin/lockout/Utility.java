package me.marin.lockout;

import me.marin.lockout.client.LockoutBoard;
import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.client.gui.BoardBuilderScreen;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.marin.lockout.Constants.*;
import static me.marin.lockout.LockoutConfig.BoardPosition.LEFT;

public class Utility {

    public static int FF000000 = 0xFF000000;

    public static void drawBingoBoard(DrawContext context) {
        LockoutConfig.BoardPosition boardPosition = LockoutConfig.getInstance().boardPosition;

        // Don't render board if F3 is open with left-side board.
        if (boardPosition == LockoutConfig.BoardPosition.LEFT && MinecraftClient.getInstance().inGameHud.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        // If in section view mode, render the section instead
        if (LockoutClient.sectionViewEnabled) {
            drawBingoBoardSection(context);
            return;
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        Lockout lockout = LockoutClient.lockout;
        LockoutBoard board = lockout.getBoard();
        float boardScale = (float) LockoutConfig.getInstance().boardScale;

        int boardWidth = 2 * GUI_PADDING + board.size() * GUI_SLOT_SIZE;
        int boardHeight = GUI_PADDING + GUI_PADDING_BOTTOM + board.size() * GUI_SLOT_SIZE;
        int scaledBoardWidth = Math.max(1, Math.round(boardWidth * boardScale));

        int boardRightEdgeX = boardPosition == LEFT ? scaledBoardWidth : context.getScaledWindowWidth();
        int boardLeftEdgeX = boardRightEdgeX - scaledBoardWidth;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(boardLeftEdgeX, 0);
        context.getMatrices().scale(boardScale, boardScale);

        int x = 0;
        int y = 0;

        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Constants.GUI_IDENTIFIER, x, y, boardWidth, boardHeight);

        x += GUI_PADDING + 1;
        y += GUI_PADDING + 1;
        final int startX = x;

        for (int i = 0; i < board.size(); i++) {
            for (int j = 0; j < board.size(); j++) {
                Goal goal = board.getGoals().get(j + board.size() * i);
                if (goal != null) {
                    if (goal.isCompleted()) {
                        context.fill(x, y, x + GUI_SLOT_SIZE, y + GUI_SLOT_SIZE, FF000000 | goal.getCompletedTeam().getColor().getColorValue());
                    }

                    goal.render(context, textRenderer, x, y);
                }
                x += GUI_SLOT_SIZE;
            }
            y += GUI_SLOT_SIZE;
            x = startX;
        }
        x += 2;
        y += 1;
        List<String> pointsList = new ArrayList<>();
        for (LockoutTeam team : lockout.getTeams()) {
            pointsList.add(team.getColor() + "" + team.getPoints() + Formatting.RESET);
        }

        context.drawText(textRenderer, String.join(Formatting.RESET + "" + Formatting.GRAY + "-", pointsList), x, y, FF000000, true);

        String timer = Utility.ticksToTimer(lockout.getTicks());
        context.drawText(textRenderer, Formatting.WHITE + timer, boardWidth - textRenderer.getWidth(timer) - 4, y, FF000000, true);

        context.getMatrices().popMatrix();

        List<String> formattedNames = new ArrayList<>();
        int maxWidth = 0;
        for (LockoutTeam team : lockout.getTeams()) {
            for (String playerName : team.getPlayerNames()) {
                formattedNames.add(team.getColor() + playerName);
                maxWidth = Math.max(maxWidth, textRenderer.getWidth(playerName));
            }
        }

        y = Math.round((GUI_PADDING + 1 + board.size() * GUI_SLOT_SIZE + 1 + 20) * boardScale);
        switch (boardPosition) {
            case RIGHT -> {
                context.fill(context.getScaledWindowWidth() - maxWidth - 3 - 1,  y - 2, context.getScaledWindowWidth() - 1, y + formattedNames.size() * textRenderer.fontHeight + 1, 0x80_00_00_00);

                for (String formattedName : formattedNames) {
                    context.drawText(textRenderer, formattedName, context.getScaledWindowWidth() - textRenderer.getWidth(formattedName) - 2, y, FF000000, true);
                    y += textRenderer.fontHeight;
                }
            }
            case LEFT -> {
                context.fill(1,  y - 2, 4 + maxWidth, y + formattedNames.size() * textRenderer.fontHeight + 1, 0x80_00_00_00);

                for (String formattedName : formattedNames) {
                    context.drawText(textRenderer, formattedName, 3, y, FF000000, true);
                    y += textRenderer.fontHeight;
                }
            }
        }

    }

    public static void drawBingoBoardSection(DrawContext context) {
        LockoutConfig.BoardPosition boardPosition = LockoutConfig.getInstance().boardPosition;

        // Don't render board if F3 is open with left-side board.
        if (boardPosition == LockoutConfig.BoardPosition.LEFT && MinecraftClient.getInstance().inGameHud.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        Lockout lockout = LockoutClient.lockout;
        LockoutBoard board = lockout.getBoard();
        float boardScale = (float) LockoutConfig.getInstance().boardScale;

        // Calculate midpoint for splitting the board in half (handles odd-sized boards)
        int boardSize = board.size();
        int midpoint = (boardSize + 1) / 2;  // Ceil division: 11->6, 12->6, 9->5

        // currentSection is 1-4 in input/UI. Convert to 0-3 for quadrant math.
        int sectionIndex = Math.max(0, Math.min(3, LockoutClient.currentSection - 1));
        int sectionRow = sectionIndex / 2;  // 0 = top, 1 = bottom
        int sectionCol = sectionIndex % 2;  // 0 = left, 1 = right

        int rowStart = (sectionRow == 0) ? 0 : midpoint;
        int rowEnd = (sectionRow == 0) ? midpoint : boardSize;
        int colStart = (sectionCol == 0) ? 0 : midpoint;
        int colEnd = (sectionCol == 0) ? midpoint : boardSize;

        int sectionHeight = rowEnd - rowStart;
        int sectionWidth = colEnd - colStart;

        int boardWidth = 2 * GUI_PADDING + sectionWidth * GUI_SLOT_SIZE;
        int boardHeight = GUI_PADDING + GUI_PADDING_BOTTOM + sectionHeight * GUI_SLOT_SIZE;
        int scaledBoardWidth = Math.max(1, Math.round(boardWidth * boardScale));

        int boardRightEdgeX = boardPosition == LEFT ? scaledBoardWidth : context.getScaledWindowWidth();
        int boardLeftEdgeX = boardRightEdgeX - scaledBoardWidth;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(boardLeftEdgeX, 0);
        context.getMatrices().scale(boardScale, boardScale);

        int x = 0;
        int y = 0;

        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Constants.GUI_IDENTIFIER, x, y, boardWidth, boardHeight);

        x += GUI_PADDING + 1;
        y += GUI_PADDING + 1;
        final int startX = x;

        // Render only the goals from the current section
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = colStart; j < colEnd; j++) {
                Goal goal = board.getGoals().get(j + boardSize * i);
                if (goal != null) {
                    if (goal.isCompleted()) {
                        context.fill(x, y, x + GUI_SLOT_SIZE, y + GUI_SLOT_SIZE, FF000000 | goal.getCompletedTeam().getColor().getColorValue());
                    }

                    goal.render(context, textRenderer, x, y);
                }
                x += GUI_SLOT_SIZE;
            }
            y += GUI_SLOT_SIZE;
            x = startX;
        }

        x += 2;
        y += 1;
        List<String> pointsList = new ArrayList<>();
        for (LockoutTeam team : lockout.getTeams()) {
            pointsList.add(team.getColor() + "" + team.getPoints() + Formatting.RESET);
        }

        context.drawText(textRenderer, String.join(Formatting.RESET + "" + Formatting.GRAY + "-", pointsList), x, y, FF000000, true);

        String timer = Utility.ticksToTimer(lockout.getTicks());
        context.drawText(textRenderer, Formatting.WHITE + timer, boardWidth - textRenderer.getWidth(timer) - 4, y, FF000000, true);

        context.getMatrices().popMatrix();

        List<String> formattedNames = new ArrayList<>();
        int maxWidth = 0;
        for (LockoutTeam team : lockout.getTeams()) {
            for (String playerName : team.getPlayerNames()) {
                formattedNames.add(team.getColor() + playerName);
                maxWidth = Math.max(maxWidth, textRenderer.getWidth(playerName));
            }
        }

        y = Math.round((GUI_PADDING + 1 + sectionHeight * GUI_SLOT_SIZE + 1 + 20) * boardScale);
        switch (boardPosition) {
            case RIGHT -> {
                context.fill(context.getScaledWindowWidth() - maxWidth - 3 - 1,  y - 2, context.getScaledWindowWidth() - 1, y + formattedNames.size() * textRenderer.fontHeight + 1, 0x80_00_00_00);

                for (String formattedName : formattedNames) {
                    context.drawText(textRenderer, formattedName, context.getScaledWindowWidth() - textRenderer.getWidth(formattedName) - 2, y, FF000000, true);
                    y += textRenderer.fontHeight;
                }
            }
            case LEFT -> {
                context.fill(1,  y - 2, 4 + maxWidth, y + formattedNames.size() * textRenderer.fontHeight + 1, 0x80_00_00_00);

                for (String formattedName : formattedNames) {
                    context.drawText(textRenderer, formattedName, 3, y, FF000000, true);
                    y += textRenderer.fontHeight;
                }
            }
        }
    }

    public static void drawCenterBingoBoard(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        // Default to applying scale
        drawCenterBingoBoard(context, textRenderer, mouseX, mouseY, true);
    }

    public static void drawCenterBingoBoard(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, boolean applyScale) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        LockoutBoard board = LockoutClient.lockout.getBoard();
        float boardScale = applyScale ? (float) LockoutConfig.getInstance().boardScale : 1.0F;

        int boardWidth = 2 * GUI_CENTER_PADDING + board.size() * GUI_CENTER_SLOT_SIZE;
        int boardHeight = 2 * GUI_CENTER_PADDING + board.size() * GUI_CENTER_SLOT_SIZE;
        int scaledBoardWidth = Math.max(1, Math.round(boardWidth * boardScale));
        int scaledBoardHeight = Math.max(1, Math.round(boardHeight * boardScale));
        int boardLeftX = width / 2 - scaledBoardWidth / 2;
        int boardTopY = height / 2 - scaledBoardHeight / 2;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(boardLeftX, boardTopY);
        context.getMatrices().scale(boardScale, boardScale);

        int x = 0;
        int y = 0;

        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, GUI_CENTER_IDENTIFIER, x, y, boardWidth, boardHeight);

        x += GUI_CENTER_PADDING + 1;
        y += GUI_CENTER_PADDING + 1;
        final int startX = x;

        Goal hoveredGoal = getBoardHoveredGoal(context, mouseX, mouseY, applyScale);

        for (int i = 0; i < board.size(); i++) {
            for (int j = 0; j < board.size(); j++) {
                Goal goal = board.getGoals().get(j + board.size() * i);
                if (goal != null) {
                    if (goal.isCompleted()) {
                        context.fill(x, y, x + GUI_CENTER_SLOT_SIZE, y + GUI_CENTER_SLOT_SIZE, (0xFF << 24) | goal.getCompletedTeam().getColor().getColorValue());
                    }

                    goal.render(context, textRenderer, x, y);

                    if (goal == hoveredGoal) {
                        context.fill(x, y, x + GUI_CENTER_SLOT_SIZE, y + GUI_CENTER_SLOT_SIZE, GUI_CENTER_HOVERED_COLOR);
                    }
                }
                x += GUI_CENTER_SLOT_SIZE;
            }
            y += GUI_CENTER_SLOT_SIZE;
            x = startX;
        }

        context.getMatrices().popMatrix();
    }

    public static Optional<Integer> getBoardHoveredIndex(int size, int width, int height, int mouseX, int mouseY) {
        return getBoardHoveredIndex(size, width, height, mouseX, mouseY, true);

    }

    public static Optional<Integer> getBoardHoveredIndex(int size, int width, int height, int mouseX, int mouseY, boolean applyScale) {
        double boardScale = applyScale ? LockoutConfig.getInstance().boardScale : 1.0;

        int boardWidth = 2 * GUI_CENTER_PADDING + size * GUI_CENTER_SLOT_SIZE;
        int boardHeight = 2 * GUI_CENTER_PADDING + size * GUI_CENTER_SLOT_SIZE;
        int scaledBoardWidth = Math.max(1, (int) Math.round(boardWidth * boardScale));
        int scaledBoardHeight = Math.max(1, (int) Math.round(boardHeight * boardScale));

        int boardLeftX = width / 2 - scaledBoardWidth / 2 - BoardBuilderScreen.CENTER_OFFSET;
        int boardTopY = height / 2 - scaledBoardHeight / 2;

        double localMouseX = (mouseX - boardLeftX) / boardScale;
        double localMouseY = (mouseY - boardTopY) / boardScale;

        int x = GUI_CENTER_PADDING + 1;
        int y = GUI_CENTER_PADDING + 1;
        final int startX = x;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (localMouseX >= x - 1 && localMouseX < x + GUI_CENTER_SLOT_SIZE && localMouseY >= y - 1 && localMouseY < y + GUI_CENTER_SLOT_SIZE) {
                    return Optional.of(j + i * size);
                }
                x += GUI_CENTER_SLOT_SIZE;
            }
            y += GUI_CENTER_SLOT_SIZE;
            x = startX;
        }

        return Optional.empty();
    }

    public static Goal getBoardHoveredGoal(DrawContext context, int mouseX, int mouseY) {
        return getBoardHoveredGoal(context, mouseX, mouseY, true);
    }

    public static Goal getBoardHoveredGoal(DrawContext context, int mouseX, int mouseY, boolean applyScale) {
        Optional<Integer> hoveredIdx = getBoardHoveredIndex(LockoutClient.lockout.getBoard().size(), context.getScaledWindowWidth(), context.getScaledWindowHeight(), mouseX, mouseY, applyScale);
        return hoveredIdx.map(integer -> LockoutClient.lockout.getBoard().getGoals().get(integer)).orElse(null);
    }

    // Draws the tooltip text when the player hover over the item slot of a goal in the Board UI
    public static void drawGoalInformation(DrawContext context, TextRenderer textRenderer, Goal goal, int mouseX, int mouseY) {
        List<OrderedText> tooltip = new ArrayList<>();
        tooltip.add(Text.of(((goal instanceof HasTooltipInfo) ? Formatting.UNDERLINE : "") + (goal.getGoalName())).asOrderedText());

        // When a goal is completed, display goal completion tooltip
        if (goal.isCompleted() && !goal.getCompletedMessage().isEmpty())
        {
            tooltip.add(Text.of( (Formatting.DARK_GRAY)+ goal.getCompletedMessage()).asOrderedText());
        }

        if (goal instanceof HasTooltipInfo) {
            String s = LockoutClient.goalTooltipMap.get(goal.getId());
            if (s != null) {
                for (String t : s.split("\n")) {
                    tooltip.add(Text.of(t).asOrderedText());
                }
            }
        }
        context.drawOrderedTooltip(textRenderer, tooltip, mouseX, mouseY);
    }

    /**
     * Code from {@link DrawContext#drawStackCount(TextRenderer, ItemStack, int, int, String)}, but without ItemStack argument requirement
     */
    public static void drawStackCount(DrawContext context, int x, int y, String count) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.0F, 0.0F);
        context.drawText(textRenderer, count, x + 19 - 2 - textRenderer.getWidth(count), y + 6 + 3, -1, true);
        context.getMatrices().popMatrix();
    }

    public static List<ServerPlayerEntity> getSpectators(Lockout lockout, MinecraftServer server) {
        return server.getPlayerManager().getPlayerList()
                .stream()
                .filter(p -> !lockout.isLockoutPlayer(p.getUuid()))
                .toList();
    }

    public static String ticksToTimer(long ticks) {
        ticks = Math.abs(ticks);
        long second = (ticks / 20) % 60;
        long minute = ((ticks / 20) / 60) % 60;
        long hour = ((ticks / 20) / 60 / 60) % 24;

        String time;
        if (hour > 0) {
            time = String.format("%02d:%02d:%02d", hour, minute, second);
        } else {
            time = String.format("%02d:%02d", minute, second);
        }

        return time;
    }

}
