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
 
 
/** Check if clazz implements Callable<? extends Serializable> and if possible cast it */
 private Class<? extends Callable<? extends Serializable>> cast(Class<?> clazz){
        if(!Callable.class.isAssignableFrom(clazz)){
            return null;
        }
        try{
            Constructor<? extends Callable<? extends Serializable>> constructor =
                    clazz.asSubclass(Callable.class).getConstructor(String.class);
            return constructor.newInstance("").getClass();
        }catch(Exception e){
            return null;
        }
    }
    
        @Override
        public Map<String, Command> scan(Class<?> clazz) {
            Class<? extends Callable<? extends Serializable>> callable = cast(clazz);
            if(callable == null){
                return Maps.newHashMap();
            }
            Command command = Command.from(callable);
            return Maps.newHashMap(command.getName(), command);
        }
    
        @Override
        public List<Class<?>> getClasses() {
            return asList(Callable.class);
        }
    
        @Override
        public Class<?> getExpectedType() {
            return EXPECTED_TYPE.getRawType();
        }
    
    }
            
 }

 

}