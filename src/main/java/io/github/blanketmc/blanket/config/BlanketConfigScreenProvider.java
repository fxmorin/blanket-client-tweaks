package io.github.blanketmc.blanket.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.blanketmc.blanket.Config;
import io.github.blanketmc.blanket.FabricClientModInitializer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.Level;

import java.util.Arrays;


/**
 * Creates a cloth-config screen for mod configs
 */
@SuppressWarnings("unchecked")
public class BlanketConfigScreenProvider implements ModMenuApi {
    private static final Config defaults = new Config();


    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> getScreen(parent, Config.config);
    }

    public static Screen getScreen(Screen parent, Config config) {
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(new TranslatableText("blanket-client-tweaks.config.title"));
        builder.setSavingRunnable(() -> ConfigHelper.saveConfig(config));

        //Config entry category
        ConfigCategory general = builder.getOrCreateCategory(new TranslatableText("blanket-client-tweaks.config.general")); //we can ignore the title, until we have more categories
        addEntriesToCategory(general, builder.entryBuilder(), config);

        return builder.build();
    }

    private static void addEntriesToCategory(ConfigCategory category, ConfigEntryBuilder entryBuilder, Config config) {
        ConfigHelper.iterateOnConfig((field, configEntry) -> {
            var type = field.getType();
            if (type.equals(Boolean.TYPE)) {

                var entry = entryBuilder.startBooleanToggle(ConfigHelper.getTextComponent(configEntry.displayName(), field.getName()), field.getBoolean(config));

                //saveConsumer
                entry.setSaveConsumer(aBoolean -> {
                    try {
                        field.set(config, aBoolean);
                    } catch(IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });

                //default value using mirror class
                boolean defVal = field.getBoolean(defaults);
                entry.setDefaultValue(defVal);

                //Description
                entry.setTooltip(fancyDescription(configEntry.description(), configEntry.categories()));

                category.addEntry(entry.build());

            } else if (type.isEnum()) {

                var clazz = (Class<Enum<?>>) type;

                Object obj = field.get(config);
                var entry = entryBuilder.startEnumSelector(ConfigHelper.getTextComponent(configEntry.displayName(), field.getName()), clazz, clazz.cast(obj));

                entry.setSaveConsumer(anEnum -> {
                    try {
                        field.set(config, anEnum);
                    } catch(IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });

                //default value using mirror class
                Object defVal = field.get(defaults);
                entry.setDefaultValue(clazz.cast(defVal));

                //Description
                entry.setTooltip(fancyDescription(configEntry.description(), configEntry.categories()));

                category.addEntry(entry.build());

            } else {
                FabricClientModInitializer.log(Level.ERROR, "Config: " + field.getName() + " can not be displayed: Unknown type", true);
            }
        });
    }

    private static Text fancyDescription(String desc, ConfigEntry.Category[] categories) {
        MutableText description = new LiteralText("");
        if (!desc.equals("")) {
            description = ConfigHelper.getTextComponent(desc, null);

            description = description.formatted(Formatting.YELLOW).append(new LiteralText("\n\n"));
        }

        description.append(new LiteralText("Categories:\n").formatted(Formatting.LIGHT_PURPLE));

        var iterator = Arrays.stream(categories).iterator();
        while (iterator.hasNext()) {

            var category = iterator.next();
            description.append(new LiteralText(category.toString()).formatted(Formatting.BLUE));

            if (iterator.hasNext()) {
                description.append(new LiteralText(" + ").formatted(Formatting.GOLD));
            }
        }
        return description;
    }

}
