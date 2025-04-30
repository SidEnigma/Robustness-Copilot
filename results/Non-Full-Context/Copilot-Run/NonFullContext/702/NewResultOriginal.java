package org.dcache.util.cli;
 
 import static java.util.Arrays.asList;
 
 import com.google.common.collect.Maps;
 import com.google.common.reflect.TypeToken;
 import dmg.util.command.Command;
 import java.io.Serializable;
 import java.lang.reflect.Constructor;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.Callable;
 
 /**
  * CommandScanner for commands implemented as non-static inner classes implementing Callable and
  * annotated with @Command.
  */
 public class AnnotatedCommandScanner implements CommandScanner {
 
     private static final TypeToken<Callable<? extends Serializable>> EXPECTED_TYPE =
           new TypeToken<Callable<? extends Serializable>>() {
           };
 
 
/** Verifies that clazz implements Callable<? extends Serializable> and casts it to that type. */

private Class<? extends Callable<? extends Serializable>> cast(Class<?> clazz) {
    if (!EXPECTED_TYPE.isAssignableFrom(TypeToken.of(clazz))) {
        throw new IllegalArgumentException("Class does not implement Callable<? extends Serializable>");
    }
    
    @SuppressWarnings("unchecked")
    Class<? extends Callable<? extends Serializable>> castedClass = (Class<? extends Callable<? extends Serializable>>) clazz;
    
    return castedClass;
}
 

}