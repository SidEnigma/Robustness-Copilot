package dmg.util.command;
 
 import com.google.common.base.Splitter;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Maps;
 import java.lang.reflect.Field;
 import java.lang.reflect.Method;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import org.dcache.util.Args;
 import org.dcache.util.cli.CommandExecutor;
 import org.dcache.util.cli.CommandScanner;
 
 /**
  * Implements the legacy cell shell commands which use reflection on method and field names.
  */
 public class AcCommandScanner implements CommandScanner {
 
     private enum FieldType {HELP_HINT, FULL_HELP, ACL}
 
     @Override
     public Map<List<String>, ? extends CommandExecutor> scan(Object obj) {
         Map<List<String>, AcCommandExecutor> commands = Maps.newHashMap();
         scanMethods(obj, commands);
         scanFields(obj, commands);
         return commands;
     }
 
 
/** Allows you to scan for help fields: fh_(= full help) or hh_(= help hint). */

private static void scanFields(Object obj, Map<List<String>, AcCommandExecutor> commands) {
    Field[] fields = obj.getClass().getDeclaredFields();
    for (Field field : fields) {
        if (field.isAnnotationPresent(HelpHint.class) || field.isAnnotationPresent(FullHelp.class)) {
            field.setAccessible(true);
            String fieldName = field.getName();
            String[] parts = fieldName.split("_");
            if (parts.length == 2) {
                String fieldType = parts[0];
                String commandName = parts[1];
                List<String> command = Splitter.on('.').splitToList(commandName);
                AcCommandExecutor executor = commands.get(command);
                if (executor == null) {
                    executor = new AcCommandExecutor();
                    commands.put(command, executor);
                }
                try {
                    if (FieldType.HELP_HINT.name().equalsIgnoreCase(fieldType)) {
                        executor.setHelpHint((String) field.get(obj));
                    } else if (FieldType.FULL_HELP.name().equalsIgnoreCase(fieldType)) {
                        executor.setFullHelp((String) field.get(obj));
                    } else if (FieldType.ACL.name().equalsIgnoreCase(fieldType)) {
                        executor.setAcl((String) field.get(obj));
                    }
                } catch (IllegalAccessException e) {
                    // Handle exception
                }
            }
        }
    }
}
 

}