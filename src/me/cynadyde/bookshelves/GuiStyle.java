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
public class GuiStyle {

    private static Map<String, GuiStyle> loadedStyles = new HashMap<>();
    private static List<String> loadedStyleNames = new ArrayList<>();
    private static Map<Integer, String> loadedDefaultNames = new HashMap<>();
    private static List<Integer> loadedDefaultSizes = new ArrayList<>();

    private static ItemStack[] navBarBackground = new ItemStack[9];
    private static int navBarUpBtnIndex = 0;
    private static int navBarHomeBtnIndex = 0;

    private static ItemStack buttonNext = new ItemStack(Material.AIR);
    private static ItemStack buttonPrev = new ItemStack(Material.AIR);
    private static ItemStack buttonUp = new ItemStack(Material.AIR);
    private static ItemStack buttonHome = new ItemStack(Material.AIR);


    /**
     * Gets a list of all currently loaded style names.
     */
    public static @NotNull List<String> getStyles() {
        return Collections.unmodifiableList(loadedStyleNames);
    }

    /**
     * Gets a style for the name if one exists. If name is null, this
     * will try to use size to get a default style.
     */
    public static @Nullable GuiStyle getStyle(@Nullable String name, int size) {

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
            // Get the default style name for that size camp...
            name = loadedDefaultNames.get(sizeCamp);
        }
        // Get the style for that name if it exists, or null...
        return loadedStyles.getOrDefault(name, null);
    }

    public static @NotNull ItemStack[] getNavBarBackground() {
        return navBarBackground;
    }

    public static int getNavBarUpBtnIndex() {
        return navBarUpBtnIndex;
    }

    public static int getNavBarHomeBtnIndex() {
        return navBarHomeBtnIndex;
    }

    public static @NotNull ItemStack getButtonPrev() {
        return buttonPrev;
    }

    public static @NotNull ItemStack getButtonNext() {
        return buttonNext;
    }

    public static @NotNull ItemStack getButtonUp() {
        return buttonUp;
    }

    public static @NotNull ItemStack getButtonHome() {
        return buttonHome;
    }

    /**
     * Gets all styles, button items, and the nav bar style from the plugin's config, logging any problems.
     */
    public static void refreshDataFromConfig(@NotNull Logger logger, @NotNull Configuration config) {

        // Reset data...
        loadedStyles.clear();
        loadedStyleNames.clear();
        loadedDefaultNames.clear();
        loadedDefaultSizes.clear();

        navBarBackground = new ItemStack[9];
        navBarUpBtnIndex = 0;
        navBarHomeBtnIndex = 0;

        buttonNext = new ItemStack(Material.AIR);
        buttonPrev = new ItemStack(Material.AIR);
        buttonUp = new ItemStack(Material.AIR);
        buttonHome = new ItemStack(Material.AIR);

        // Get the GUI styles section of the config...
        ConfigurationSection ymlGuiStyles = config.getConfigurationSection("gui-styles");
        if (ymlGuiStyles == null) {
            logger.warning("[Config] *** Maligned config: missing 'gui-styles' section! ***");
            logger.warning("[Config] No styles could be loaded.");
            return;
        }
        // For each style defined in the config...
        stylesLoop:
        for (String styleName : ymlGuiStyles.getKeys(false)) {

            // Get the style if it is exists, else return null...
            ConfigurationSection ymlStyle = ymlGuiStyles.getConfigurationSection(styleName);
            if (ymlStyle == null) {
                logger.warning("[Config] Maligned config: null style.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }
            // Get the style pattern...
            String pattern = ymlStyle.getString("pattern");
            if (pattern == null) {
                logger.warning("[Config] Maligned config: null style pattern.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }
            if (pattern.length() % 9 != 0 || pattern.length() < 9 || pattern.length() > 81) {
                logger.warning("[Config] Maligned config: invalid style pattern.");
                logger.warning("[Config] Pattern length must be a multiple of 9 from 9 to 81.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }
            // Get the style pattern key...
            Map<Character, Material> key = new HashMap<>();
            ConfigurationSection ymlStyleKey = ymlStyle.getConfigurationSection("key");
            if (ymlStyleKey == null) {
                logger.warning("[Config] Maligned config: null style key.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }
            // For pattern-char in key...
            for (String keyChar : ymlStyleKey.getKeys(false)) {
                if (keyChar.length() != 1) {
                    logger.warning("[Config] Maligned config: style key char must have a length of 1.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                    continue stylesLoop;
                }
                // Get the pattern-char's material...
                String keyMat = ymlStyleKey.getString(keyChar);
                if (keyMat == null) {
                    logger.warning("[Config] Maligned config: null style key material.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                    continue stylesLoop;
                }

                // Convert the material into an enum constant...
                try {
                    key.put(keyChar.charAt(0), Material.valueOf(keyMat.toUpperCase()));
                }
                catch (IllegalArgumentException ex) {
                    logger.warning("[Config] Maligned config: invalid style key material.");
                    logger.warning("[Config] Material must be a valid minecraft identifier.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                    continue stylesLoop;
                }
            }
            // Get the prev-button value...
            if (!ymlStyle.contains("prev-button")) {
                logger.warning("[Config] Maligned config: missing prev-button index.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }
            int prevButton = ymlStyle.getInt("prev-button");
            if (prevButton < 0 || prevButton >= pattern.length()) {
                logger.warning("[Config] Maligned config: invalid prev-button index.");
                logger.warning("[Config] Indexes must be between 0 and pattern length.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }

            // Get the next-button value...
            if (!ymlStyle.contains("next-button")) {
                logger.warning("[Config] Maligned config: missing next-button index.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }
            int nextButton = ymlStyle.getInt("next-button");
            if (nextButton < 0 || nextButton >= pattern.length()) {
                logger.warning("[Config] Maligned config: invalid prev-button index.");
                logger.warning("[Config] Indexes must be between 0 and pattern length.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }

            // Get the content values....
            if (!ymlStyle.contains("content")) {
                logger.warning("[Config] Maligned config: missing list of content indexes.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                continue;
            }
            List<Integer> contentList = ymlStyle.getIntegerList("content");

            int[] content = new int[contentList.size()];
            for (int i = 0; i < content.length; i++) {
                Integer index = contentList.get(i);
                if (index == null) {
                    logger.warning("[Config] Maligned config: null content index.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                    continue stylesLoop;
                }
                if (index < 0 || index >= pattern.length()) {
                    logger.warning("[Config] Maligned config: invalid content index.");
                    logger.warning("[Config] Indexes must be between 0 and pattern length.");
                    logger.warning(String.format("[Config] Skipped %s.%s...", "gui-styles", styleName));
                    continue stylesLoop;
                }
                content[i] = index;
            }
            // Put the valid GUI style in the plugin cache...
            loadedStyles.put(styleName, new GuiStyle(pattern, key, prevButton, nextButton, content));

            // Update the list of style names...
            loadedStyleNames.add(styleName);
        }

        // Store the list of default style sizes...
        ConfigurationSection ymlDefaultGui = config.getConfigurationSection("default-gui");
        if (ymlDefaultGui == null) {
            logger.warning("[Config] *** Maligned config: missing 'default-gui' section. ***");
            logger.warning("[Config] No default GUI styles can be applied.");
            return;
        }
        // Store the default style name for each size...
        for (String sizeKey : ymlDefaultGui.getKeys(false)) {
            try {
                Integer size = Integer.valueOf(sizeKey);
                if (size < 0) {
                    throw new IllegalArgumentException();
                }
                loadedDefaultNames.put(size, ymlDefaultGui.getString(sizeKey));
                loadedDefaultSizes.add(size);
            }
            catch (IllegalArgumentException ignored) {
                logger.warning("[Config] Maligned config: invalid default-gui size key.");
                logger.warning("[Config] The default-gui size key must be a non-negative integer.");
                logger.warning(String.format("[Config] Skipped %s.%s...", "default-gui", sizeKey));
            }
        }
        // Sort the list of default style sizes...
        if (loadedDefaultSizes.size() > 1) {
            Collections.sort(loadedDefaultSizes);
        }

        navStyle:
        {
            // Get the nav bar style section...
            ConfigurationSection ymlNavStyle = config.getConfigurationSection("nav-style");
            if (ymlNavStyle == null) {
                logger.warning("[Config] Maligned config: missing 'nav-style' section.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }

            // Get the nav bar pattern...
            String navPattern = ymlNavStyle.getString("pattern");
            if (navPattern == null) {
                logger.warning("[Config] Maligned config: null nav-style pattern.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }
            if (navPattern.length() != 9) {
                logger.warning("[Config] Maligned config: invalid nav-style pattern.");
                logger.warning("[Config] nav-style pattern length must be exactly 9.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }

            // Get the nav bar key...
            Map<Character, Material> navKey = new HashMap<>();
            ConfigurationSection ymlNavStyleKey = config.getConfigurationSection("key");
            if (ymlNavStyleKey == null) {
                logger.warning("[Config] Maligned config: null nav-style key.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }
            // For pattern-char in key...
            for (String keyChar : ymlNavStyleKey.getKeys(false)) {

                if (keyChar.length() != 1) {
                    logger.warning("[Config] Maligned config: nav-style key char must have a length of 1.");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break navStyle;
                }
                // Get the pattern-char's material...
                String keyMat = ymlNavStyleKey.getString(keyChar);
                if (keyMat == null) {
                    logger.warning("[Config] Maligned config: null nav-style key material.");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break navStyle;
                }
                // Convert the material into an enum constant...
                try {
                    navKey.put(keyChar.charAt(0), Material.valueOf(keyMat.toUpperCase()));
                }
                catch (IllegalArgumentException ex) {
                    logger.warning("[Config] Maligned config: invalid nav-style key material.");
                    logger.warning("[Config] Material must be a valid minecraft identifier.");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break navStyle;
                }
            }
            // Create the nav bar background...
            ItemStack[] background = new ItemStack[navPattern.length()];
            for (int i = 0; i < background.length; i++) {
                background[i] = Utils.itemStack(1, navKey.get(navPattern.charAt(i)), " ");
            }
            navBarBackground = background;

            // Get the nav bar up button index...
            if (!ymlNavStyle.contains("up-button")) {
                logger.warning("[Config] Maligned config: missing nav-style up-button index.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }
            int upButton = ymlNavStyle.getInt("up-button");
            if (upButton < 0 || upButton >= navPattern.length()) {
                logger.warning("[Config] Maligned config: invalid nav-style up-button index.");
                logger.warning("[Config] Indexes must be between 0 and pattern length.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }
            navBarUpBtnIndex = upButton;

            // Get the nav bar home button index...
            if (!ymlNavStyle.contains("home-button")) {
                logger.warning("[Config] Maligned config: missing nav-style home-button index.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }
            int homeButton = ymlNavStyle.getInt("up-button");
            if (homeButton < 0 || homeButton >= navPattern.length()) {
                logger.warning("[Config] Maligned config: invalid nav-style home-button index.");
                logger.warning("[Config] Indexes must be between 0 and pattern length.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break navStyle;
            }
            navBarHomeBtnIndex = homeButton;
        }
        buttonItems:
        {
            // Get the button items section...
            ConfigurationSection ymlButtons = config.getConfigurationSection("buttons");
            if (ymlButtons == null) {
                logger.warning("[Config] Maligned config: missing 'buttons' section.");
                logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                break buttonItems;
            }
            // Set the previous-button item...
            prevButton:
            {
                String buttonItemString = ymlButtons.getString("prev-button");
                if (buttonItemString == null) {
                    logger.warning("[Config] Maligned config: missing 'buttons.prev-button'.");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break prevButton;
                }
                ItemStack buttonItem = Utils.buildItem(buttonItemString);
                if (buttonItem == null) {
                    logger.warning("[Config] Maligned config: invalid value for 'buttons.prev-button'.");
                    logger.warning("[Config] Must be of the form: <material-name> <display-name>");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break prevButton;
                }
                buttonPrev = buttonItem;
            }
            // Set the next-button item...
            nextButton:
            {
                String buttonItemString = ymlButtons.getString("next-button");
                if (buttonItemString == null) {
                    logger.warning("[Config] Maligned config: missing 'buttons.next-button'.");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break nextButton;
                }
                ItemStack buttonItem = Utils.buildItem(buttonItemString);
                if (buttonItem == null) {
                    logger.warning("[Config] Maligned config: invalid value for 'buttons.next-button'.");
                    logger.warning("[Config] Must be of the form: <material-name> <display-name>");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break nextButton;
                }
                buttonNext = buttonItem;
            }
            // Set the up-button item...
            upButton:
            {
                String buttonItemString = ymlButtons.getString("up-button");
                if (buttonItemString == null) {
                    logger.warning("[Config] Maligned config: missing 'buttons.up-button'.");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break upButton;
                }
                ItemStack buttonItem = Utils.buildItem(buttonItemString);
                if (buttonItem == null) {
                    logger.warning("[Config] Maligned config: invalid value for 'buttons.up-button'.");
                    logger.warning("[Config] Must be of the form: <material-name> <display-name>");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break upButton;
                }
                buttonUp = buttonItem;
            }
            // Set the home-button item...
            homeButton:
            {
                String buttonItemString = ymlButtons.getString("home-button");
                if (buttonItemString == null) {
                    logger.warning("[Config] Maligned config: missing 'buttons.home-button'.");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break homeButton;
                }
                ItemStack buttonItem = Utils.buildItem(buttonItemString);
                if (buttonItem == null) {
                    logger.warning("[Config] Maligned config: invalid value for 'buttons.home-button'.");
                    logger.warning("[Config] Must be of the form: <material-name> <display-name>");
                    logger.warning("[Config] GUIs will not display properly. Remove broken config file.");
                    break homeButton;
                }
                buttonHome = buttonItem;
            }
        }
    }

    private int prevBtnIndex;
    private int nextBtnIndex;
    private int[] contentIndices;
    private ItemStack[] background;

    private GuiStyle(String pattern, Map<Character, Material> key, int prevBtnIndex, int nextBtnIndex, int[] contentIndices) {

        this.prevBtnIndex = prevBtnIndex;
        this.nextBtnIndex = nextBtnIndex;
        this.contentIndices = contentIndices;

        // Create the style background from its pattern and key...
        this.background = new ItemStack[pattern.length()];
        for (int i = 0; i < this.background.length; i++) {
            this.background[i] = Utils.itemStack(1, key.get(pattern.charAt(i)), " ");
        }
    }

    public int prevBtnIndex() {
        return prevBtnIndex;
    }

    public int nextBtnIndex() {
        return nextBtnIndex;
    }

    public @NotNull int[] contentIndices() {
        return contentIndices;
    }

    public @NotNull ItemStack[] getBackground() {
        return background;
    }
}
