package edu.harvard.iq.dataverse.validation;
 
 import edu.harvard.iq.dataverse.util.BundleUtil;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.logging.Logger;
 import org.apache.commons.lang3.StringUtils;
 import org.passay.CharacterRule;
 import org.passay.EnglishCharacterData;
 
 public class PasswordValidatorUtil {
 
     private static final Logger logger = Logger.getLogger(PasswordValidatorUtil.class.getCanonicalName());
 
     // TODO: Work on switching ILLEGAL_MATCH to something like TOO_MANY_DIGITS.
     public enum ErrorType {
         TOO_SHORT, INSUFFICIENT_CHARACTERISTICS, ILLEGAL_MATCH, ILLEGAL_WORD
     };
 
     public static List<CharacterRule> getCharacterRules(String configString) {
         if (configString == null || configString.isEmpty()) {
             return getCharacterRulesDefault(); //sane default
         } else {
             List<CharacterRule> rules = parseConfigString(configString);
             return rules;
         }
     }
 
     /**
      * The default out-of-the-box character rules for Dataverse.
      */
     public static List<CharacterRule> getCharacterRulesDefault() {
         List<CharacterRule> characterRules = new ArrayList<>();
         characterRules.add(new CharacterRule(EnglishCharacterData.Alphabetical, 1));
         characterRules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
         return characterRules;
     }
 
 
/** Parses the list of character rules as defined in the database. */
 public static List<CharacterRule> parseConfigString(String configString){
        List<CharacterRule> rules = new ArrayList<>();
        String[] ruleStrings = configString.split(",");
        for (String ruleString : ruleStrings) {
            String[] ruleParts = ruleString.split(":");
            if (ruleParts.length == 2) {
                String ruleName = ruleParts[0];
                String ruleCount = ruleParts[1];
                CharacterRule rule = new CharacterRule(ruleName, Integer.parseInt(ruleCount));
                rules.add(rule);
            } else {
                logger.warning("Invalid character rule: " + ruleString);
            }
        }
        return rules;
    }
    
        public static List<String> getErrorMessages(List<ErrorType> errorTypes) {
            List<String> errorMessages = new ArrayList<>();
            for (ErrorType errorType : errorTypes) {
                errorMessages.add(getErrorMessage(errorType));
            }
            return errorMessages;
        }
    
        public static String getErrorMessage(ErrorType errorType) {
            switch (errorType) {
                case TOO_SHORT:
                    return BundleUtil.getStringFromBundle("password.too.short");
                case INSUFFICIENT_CHARACTERISTICS:
                    return BundleUtil.getStringFromBundle("password.insufficient.characteristics");
                case ILLEGAL_MATCH:
                    return BundleUtil.getStringFromBundle("password.illegal.match");
                case ILLEGAL_WORD:
                    return BundleUtil.getStringFromBundle("password.illegal.word");
                default:
                    return "";
            }
        }
    
        public static List<ErrorType> getErrorTypes(String password) {
            List<ErrorType> errorTypes = new ArrayList<>();
            if (password.length() < 8) {
                errorTypes.add(ErrorType.TOO_SHORT);
            }
            if (password.length() >= 8 && password.length() < 16) {
                if (!hasEnoughCharacteristics(password)) {
                    errorTypes.     
 }

 

}