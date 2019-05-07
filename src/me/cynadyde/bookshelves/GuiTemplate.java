package me.cynadyde.bookshelves;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings({ "unused", "WeakerAccess" })
public class GuiTemplate {

    private static Map<String, GuiTemplate> loadedTemplates = new HashMap<>();
    private static Map<Integer, String> loadedDefaults = new HashMap<>();
    private static List<Integer> loadedDefaultSizes = new ArrayList<>();

    public static @NotNull Set<String> getTemplates() {
        return loadedTemplates.keySet();
    }

    public static @Nullable GuiTemplate getTemplate(@Nullable String name, int size) {

        // If no name was given...
        if (name == null) {

            if (loadedDefaultSizes.isEmpty()) {
                return null;
            }
            // Get the size camp for the given size...
            int sizeCamp = loadedDefaultSizes.get(0);

            for (int camp : loadedDefaultSizes) {
                if (size >= camp) {
                    sizeCamp = camp;
                }
            }
            // Get the default template name for that size camp...
            name = loadedDefaults.get(sizeCamp);
        }
        // Get the template for that name if it exists, or null...
        return loadedTemplates.getOrDefault(name, null);
    }

    public static void refreshDataFromConfig(@NotNull Logger logger, @NotNull Configuration config) {

        loadedTemplates.clear();
        loadedDefaults.clear();
        loadedDefaultSizes.clear();

        // Get the GUI templates section of the config...
        ConfigurationSection ymlGuiTemplates = config.getConfigurationSection("gui-templates");
        if (ymlGuiTemplates == null) {
            logger.warning("[Config] *** Maligned config: missing 'gui-templates' section! ***");
            logger.warning("[Config] No templates could be loaded.");
            return;
        }
        // For each template key defined in the config...
        templatesLoop:
        for (String templateName : ymlGuiTemplates.getKeys(false)) {

            // Get the template if it is exists, else return null...
            ConfigurationSection ymlTemplate = ymlGuiTemplates.getConfigurationSection(templateName);
            if (ymlTemplate == null) {
                logger.warning("[Config] Maligned config: null template.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }
            // Get the pattern value...
            String pattern = ymlTemplate.getString("pattern");
            if (pattern == null) {
                logger.warning("[Config] Maligned config: null template pattern.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }
            if (pattern.length() % 9 != 0 || pattern.length() < 9 || pattern.length() > 81) {
                logger.warning("[Config] Maligned config: invalid template pattern length.");
                logger.warning("[Config] Pattern length must be either: 9, 18, 27, 36, 45, 54, 63, 72, or 81.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }
            // Get the template key...
            Map<Character, Material> key = new HashMap<>();
            ConfigurationSection ymlTemplateKey = ymlTemplate.getConfigurationSection("key");
            if (ymlTemplateKey == null) {
                logger.warning("[Config] Maligned config: null template key section.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }
            // For pattern char pair in key...
            for (String keyChar : ymlTemplateKey.getKeys(false)) {
                if (keyChar.length() < 1) {
                    logger.warning("[Config] Maligned config: empty template key char.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                    continue templatesLoop;
                }
                // Get the the pattern material...
                String matVal = ymlTemplateKey.getString(keyChar);
                if (matVal == null) {
                    logger.warning("[Config] Maligned config: null template key material.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                    continue templatesLoop;
                }

                // Convert the pattern material into an enum constant...
                Material mat;
                try {
                    if (!matVal.startsWith("minecraft:")) {
                        throw new IllegalArgumentException();
                    }
                    mat = Material.valueOf(matVal.substring("minecraft:".length()));
                    key.put(keyChar.charAt(0), mat);
                }
                catch (IllegalArgumentException ex) {
                    logger.warning("[Config] Maligned config: invalid template key material.");
                    logger.warning("[Config] Material must be a valid minecraft identifier.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                    continue templatesLoop;
                }
            }
            // Get the prev-button value...
            if (!ymlTemplate.contains("prev-buttons")) {
                logger.warning("[Config] Maligned config: missing prev-button index.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }
            int prevButton = ymlTemplate.getInt("prev-button");
            if (prevButton < 0 || prevButton >= pattern.length()) {
                logger.warning("[Config] Maligned config: invalid prev-button index.");
                logger.warning("[Config] Indexes must be between 0 and pattern length.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }

            // Get the next-button value...
            if (!ymlTemplate.contains("next-buttons")) {
                logger.warning("[Config] Maligned config: missing next-button index.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }
            int nextButton = ymlTemplate.getInt("next-button");
            if (nextButton < 0 || nextButton >= pattern.length()) {
                logger.warning("[Config] Maligned config: invalid prev-button index.");
                logger.warning("[Config] Indexes must be between 0 and pattern length.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }

            // Get the content values....
            if (!ymlTemplate.contains("content")) {
                logger.warning("[Config] Maligned config: missing list of content indexes.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                continue;
            }
            List<Integer> contentList = ymlTemplate.getIntegerList("content");

            int[] content = new int[contentList.size()];
            for (int i = 0; i < content.length; i++) {
                Integer index = contentList.get(i);
                if (index == null) {
                    logger.warning("[Config] Maligned config: null content index.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                    continue templatesLoop;
                }
                content[i] = index;
                if (index < 0 || index >= pattern.length()) {
                    logger.warning("[Config] Maligned config: invalid content index.");
                    logger.warning("[Config] Indexes must be between 0 and pattern length.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-templates", templateName));
                    continue templatesLoop;
                }
            }
            // Put the valid GUI template in the plugin cache...
            loadedTemplates.put(templateName, new GuiTemplate(pattern, key, prevButton, nextButton, content));
        }

        // Cache a list of the default template sizes...
        ConfigurationSection ymlDefaultGui = config.getConfigurationSection("default-gui");
        if (ymlDefaultGui == null) {
            logger.warning("[Config] *** Maligned config: missing 'default-gui' section. ***");
            logger.warning("[Config] No default GUI templates can be applied.");
            return;
        }
        // Add the default template for each size to the cache...
        for (String key : ymlDefaultGui.getKeys(false)) {
            try {
                Integer sizeCamp = Integer.valueOf(key);
                if (sizeCamp < 0) {
                    throw new IllegalArgumentException();
                }
                loadedDefaults.put(sizeCamp, ymlDefaultGui.getString(key));
                loadedDefaultSizes.add(sizeCamp);
            }
            catch (IllegalArgumentException ignored) {
                logger.warning("[Config] Maligned config: invalid default-gui key.");
                logger.warning("[Config] The default-gui key must be a non-negative integer.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "default-gui", key));
            }
        }
        // Sort the cached list of default template sizes...
        if (loadedDefaultSizes.size() > 1) {
            Collections.sort(loadedDefaultSizes);
        }
    }

    private String pattern;
    private Map<Character, Material> key;
    private int prevButton;
    private int nextButton;
    private int[] content;
    private ItemStack[] background;

    private GuiTemplate(String pattern, Map<Character, Material> key, int prevButton, int nextButton, int[] content) {

        this.pattern = pattern;
        this.key = key;
        this.prevButton = prevButton;
        this.nextButton = nextButton;
        this.content = content;

        // Create the items array for the template...
        this.background = new ItemStack[this.pattern.length()];
        for (int i = 0; i < this.background.length; i++) {
            this.background[i] = Utils.itemStack(1, this.key.get(pattern.charAt(i)), " ");
        }
    }

    public @NotNull String getPattern() {
        return pattern;
    }

    public @NotNull Map<Character, Material> getKey() {
        return key;
    }

    public int getPrevButton() {
        return prevButton;
    }

    public int getNextButton() {
        return nextButton;
    }

    public @NotNull int[] getContent() {
        return content;
    }

    public @NotNull ItemStack[] getBackground() {
        return background;
    }
}
