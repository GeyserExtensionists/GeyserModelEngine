package re.imc.geysermodelengine.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class ColourUtils {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public @NotNull Component miniFormat(String message) {
        return miniMessage.deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public @NotNull Component miniFormat(String message, TagResolver tagResolver) {
        return miniMessage.deserialize(message, tagResolver).decoration(TextDecoration.ITALIC, false);
    }

    public String stripColour(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}
