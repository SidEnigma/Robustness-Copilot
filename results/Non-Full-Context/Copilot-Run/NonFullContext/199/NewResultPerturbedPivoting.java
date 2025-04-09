package net.glowstone.entity;
 
 import com.flowpowered.network.Message;
 import com.google.common.collect.Maps;
 import lombok.AllArgsConstructor;
 import lombok.Getter;
 import lombok.RequiredArgsConstructor;
 import net.glowstone.net.GlowSession;
 import net.glowstone.net.message.play.entity.EntityPropertyMessage;
 import org.bukkit.attribute.Attribute;
 import org.bukkit.attribute.AttributeInstance;
 import org.bukkit.attribute.AttributeModifier;
 
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Map;
 import java.util.UUID;
 import java.util.function.Function;
 import java.util.stream.Collectors;
 
 /**
  * Manages the attributes described at https://minecraft.gamepedia.com/Attribute
  */
 public class AttributeManager {
 
     private final GlowLivingEntity entity;
     private final Map<String, Property> properties;
 
     private boolean needsUpdate;
 
     /**
      * Create an instance for the given entity.
      *
      * @param entity the entity whose attributes will be managed
      */
     public AttributeManager(GlowLivingEntity entity) {
         this.entity = entity;
         properties = Maps.newHashMap();
         needsUpdate = false;
     }
 
     /**
      * Adds an {@link EntityPropertyMessage} with our entity's properties to the given collection of
      * messages, if the client's snapshot is stale.
      *
      * @param messages the message collection to add to
      */
     public void applyMessages(Collection<Message> messages) {
         if (!needsUpdate) {
             return;
         }
         messages.add(new EntityPropertyMessage(entity.entityId, properties));
         needsUpdate = false;
     }
 
 
/** Sends the properties of the managed entity to the client, if the client snapshot is outdated. */

public void sendMessages(GlowSession session) {
    if (!needsUpdate) {
        return;
    }
    Collection<Message> messages = new ArrayList<>();
    messages.add(new EntityPropertyMessage(entity.entityId, properties));
    session.send(messages);
    needsUpdate = false;
}
 

}