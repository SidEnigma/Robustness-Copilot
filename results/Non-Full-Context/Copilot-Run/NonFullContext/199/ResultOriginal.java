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
 
 
/** Sends the managed entity's properties to the client, if the client's snapshot is stale. */
 public void sendMessages(GlowSession session){
        if (!needsUpdate) {
            return;
        }
        session.send(new EntityPropertyMessage(entity.entityId, properties));
        needsUpdate = false;
    }
    
        /**
        * Sets the value of the given property.
        *
        * @param name the name of the property
        * @param value the value of the property
        */
        public void set(String name, double value) {
            Property property = properties.get(name);
            if (property == null) {
                property = new Property(name, value);
                properties.put(name, property);
            } else {
                property.setValue(value);
            }
            needsUpdate = true;
        }
    
        /**
        * Gets the value of the given property.
        *
        * @param name the name of the property
        * @return the value of the property
        */
        public double get(String name) {
            Property property = properties.get(name);
            if (property == null) {
                return 0;
            }
            return property.getValue();
        }
    
        /**
        * Gets the value of the given property.
        *
        * @param name the name of the property
        * @return the value of the property
        */
        public double get(Attribute attribute) {
            return get(attribute.getName());
        }
    
        /**
        * Gets the value of the given property.
        *
        * @param attribute the attribute to get the value of
        * @return the value of the property
        */
        public double get(AttributeInstance attribute) {
            return get(attribute.getAttribute());
        }
    
        /**
        * Gets the value of the given property.
        *
        * @param attribute the attribute to get the value of
        * @return the value of the property
        */
        public double get(AttributeModifier modifier) {
            return get(modifier.getAttribute());
        }
    
        /**
        * Gets the value of the given property.
        *
        * @param attribute the attribute to get the value of
        *       
 }

 

}