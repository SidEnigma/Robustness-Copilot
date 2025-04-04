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
 
 
/** Scan for help fields: fh_(= full help) or hh_(= help hint). */
 private static void scanFields(Object obj, Map<List<String>, AcCommandExecutor> commands){}

 

}