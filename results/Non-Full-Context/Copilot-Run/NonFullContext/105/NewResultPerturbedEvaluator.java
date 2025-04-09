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
 
 
/** For the given change name, create a new implementation of Change. A new instance of Change will be returned for each call to create. */

public Change create(String name) {
    ServiceLocator serviceLocator = Scope.getCurrentScope().getServiceLocator();
    ChangeFactory changeFactory = serviceLocator.getChangeFactory();

    for (ChangeMetaData metaData : changeFactory.metaDataByClass.values()) {
        if (metaData.getName().equals(name)) {
            try {
                return metaData.getChangeClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new UnexpectedLiquibaseException("Failed to create instance of Change: " + name, e);
            }
        }
    }

    throw new UnexpectedLiquibaseException("No implementation found for Change: " + name);
}
 

}