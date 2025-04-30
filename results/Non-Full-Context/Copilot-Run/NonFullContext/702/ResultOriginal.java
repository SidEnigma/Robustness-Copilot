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
 private Class<? extends Callable<? extends Serializable>> cast(Class<?> clazz){
        if(!Callable.class.isAssignableFrom(clazz)){
            throw new IllegalArgumentException("Class " + clazz + " does not implement Callable");
        }
        if(!EXPECTED_TYPE.isAssignableFrom(TypeToken.of(clazz))){
            throw new IllegalArgumentException("Class " + clazz + " does not implement Callable<? extends Serializable>");
        }
        return clazz.asSubclass(Callable.class);
    }
    
        @Override
        public Map<String, Command> scan(Class<?> clazz) {
            Map<String, Command> commands = Maps.newHashMap();
            for(Class<?> c : asList(clazz.getDeclaredClasses())){
                if(Callable.class.isAssignableFrom(c)){
                    Callable<? extends Serializable> callable = cast(c).cast(c.getDeclaredConstructor().newInstance());
                    Command command = new Command(callable);
                    commands.put(command.getName(), command);
                }
            }
            return commands;
        }       
 }

 

}