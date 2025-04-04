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
 
 
/** Look for the help fields: fh_(= full help) or hh_(= help index). */

private static void scanFields(Object obj, Map<List<String>, AcCommandExecutor> commands) {
    Field[] fields = obj.getClass().getDeclaredFields();
    for (Field field : fields) {
        if (field.isAnnotationPresent(HelpHint.class) || field.isAnnotationPresent(FullHelp.class) || field.isAnnotationPresent(ACL.class)) {
            field.setAccessible(true);
            String fieldName = field.getName();
            String[] fieldParts = fieldName.split("_");
            if (fieldParts.length == 2) {
                String fieldType = fieldParts[0];
                String commandName = fieldParts[1];
                List<String> command = Splitter.on('.').splitToList(commandName);
                AcCommandExecutor executor = commands.get(command);
                if (executor == null) {
                    executor = new AcCommandExecutor();
                    commands.put(command, executor);
                }
                try {
                    if (fieldType.equals(FieldType.HELP_HINT.name())) {
                        executor.setHelpHint((String) field.get(obj));
                    } else if (fieldType.equals(FieldType.FULL_HELP.name())) {
                        executor.setFullHelp((String) field.get(obj));
                    } else if (fieldType.equals(FieldType.ACL.name())) {
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