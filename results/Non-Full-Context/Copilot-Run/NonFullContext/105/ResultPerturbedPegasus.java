package liquibase.change;
 
 import liquibase.Scope;
 import liquibase.exception.UnexpectedLiquibaseException;
 import liquibase.plugin.AbstractPluginFactory;
 import liquibase.plugin.Plugin;
 import liquibase.servicelocator.ServiceLocator;
 
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;
 
 /**
  * Factory class for constructing the correct liquibase.change.Change implementation based on a command name.
  * For XML-based changelogs, the tag name is the command name.
  * Change implementations are looked up via the {@link ServiceLocator}.
  *
  * @see liquibase.change.Change
  */
 public class ChangeFactory extends AbstractPluginFactory<Change>{
 
     private Map<Class<? extends Change>, ChangeMetaData> metaDataByClass = new ConcurrentHashMap<>();
 
     private ChangeFactory() {
 
     }
 
     @Override
     protected Class<Change> getPluginClass() {
         return Change.class;
     }
 
     @Override
     protected int getPriority(Change obj, Object... args) {
         String commandName = (String) args[0];
         ChangeMetaData changeMetaData = getChangeMetaData(obj);
         if (commandName.equals(changeMetaData.getName())) {
             return changeMetaData.getPriority();
         } else {
             return Plugin.PRIORITY_NOT_APPLICABLE;
         }
     }
 
     public ChangeMetaData getChangeMetaData(String change) {
         Change changeObj = create(change);
         if (changeObj == null) {
             return null;
         }
         return getChangeMetaData(changeObj);
     }
 
     public ChangeMetaData getChangeMetaData(Change change) {
         if (!metaDataByClass.containsKey(change.getClass())) {
             metaDataByClass.put(change.getClass(), change.createChangeMetaData());
         }
         return metaDataByClass.get(change.getClass());
     }
 
 
     /**
      * Unregister all instances of a given Change name. Normally used for testing, but can be called manually if needed.
      */
     public void unregister(String name) {
         for (Change change : new ArrayList<>(findAllInstances())) {
             if (getChangeMetaData(change).getName().equals(name)) {
                 this.removeInstance(change);
             }
         }
     }
 
     /**
      * Returns all defined changes in the registry. Returned set is not modifiable.
      */
     public SortedSet<String> getDefinedChanges() {
         SortedSet<String> names = new TreeSet<>();
         for (Change change : findAllInstances()) {
             names.add(getChangeMetaData(change).getName());
         }
         return Collections.unmodifiableSortedSet(names);
     }
 
 
/** A new implementation for the given change name is needed. */
 public Change create(String name){
        Change change = createInstance(name);
        if (change == null) {
            throw new UnexpectedLiquibaseException("No implementation found for change " + name);
        }
        return change;
    }
    
        public Change create(String name, Scope scope) {
            Change change = create(name);
            change.setScope(scope);
            return change;
        }
    
        public Change create(String name, Scope scope, String parameter) {
            Change change = create(name, scope);
            change.setParameter(parameter);
            return change;
        }
    
        public Change create(String name, Scope scope, String parameter, String parameter2) {
            Change change = create(name, scope);
            change.setParameter(parameter);
            change.setParameter2(parameter2);
            return change;
        }
    
        public Change create(String name, Scope scope, String parameter, String parameter2, String parameter3) {
            Change change = create(name, scope);
            change.setParameter(parameter);
            change.setParameter2(parameter2);
            change.setParameter3(parameter3);
            return change;
        }
    
        public Change create(String name, Scope scope, String parameter, String parameter2, String parameter3, String parameter4) {
            Change change = create(name, scope);
            change.setParameter(parameter);
            change.setParameter2(parameter2);
            change.setParameter3(parameter3);
            change.setParameter4(parameter4);
            return change;
        }
    
        public Change create(String name, Scope scope, String parameter, String parameter2, String parameter3, String parameter4, String parameter5) {
            Change change = create(name, scope);
            change.setParameter(parameter);
            change.setParameter2(parameter2);
            change.setParameter3(parameter3);
            change.setParameter4(parameter4);
            change.setParameter5(parameter5);
            return change;
        }       
 }

 

}