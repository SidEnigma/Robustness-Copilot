package liquibase.precondition;
 
 import liquibase.Scope;
 import liquibase.exception.UnexpectedLiquibaseException;
 import liquibase.servicelocator.ServiceLocator;
 
 import java.util.HashMap;
 import java.util.Map;
 
 public class PreconditionFactory {
     @SuppressWarnings("unchecked")
     private final Map<String, Class<? extends Precondition>> preconditions;
 
     private static PreconditionFactory instance;
 
     @SuppressWarnings("unchecked")
     private PreconditionFactory() {
         preconditions = new HashMap<>();
         try {
             for (Precondition precondition : Scope.getCurrentScope().getServiceLocator().findInstances(Precondition.class)) {
                 register(precondition);
             }
         } catch (Exception e) {
             throw new UnexpectedLiquibaseException(e);
         }
     }
 
     public static synchronized PreconditionFactory getInstance() {
         if (instance == null) {
             instance = new PreconditionFactory();
         }
         return instance;
     }
 
     public static synchronized void reset() {
         instance = new PreconditionFactory();
     }
 
     public Map<String, Class<? extends Precondition>> getPreconditions() {
         return preconditions;
     }
 
     public void register(Precondition precondition) {
         try {
             preconditions.put(precondition.getName(), precondition.getClass());
         } catch (Exception e) {
             throw new UnexpectedLiquibaseException(e);
         }
     }
 
     public void unregister(String name) {
         preconditions.remove(name);
     }
 
 
/** Starting from the given tag name, create a new Precondition subclass. */
 public Precondition create(String tagName){
        try {
            Class<? extends Precondition> preconditionClass = preconditions.get(tagName);
            if (preconditionClass == null) {
                throw new UnexpectedLiquibaseException("No precondition found for tag " + tagName);
            }
            return preconditionClass.newInstance();
        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }       
 }

 

}