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
 
 
/** The list of character rules isParsed. */
 public static List<CharacterRule> parseConfigString(String configString){
        List<CharacterRule> characterRules = new ArrayList<>();
        String[] rules = configString.split(",");
        for (String rule : rules) {
            String[] ruleParts = rule.split(":");
            if (ruleParts.length == 2) {
                String ruleName = ruleParts[0];
                String ruleValue = ruleParts[1];
                if (ruleName.equals("Alphabetical")) {
                    characterRules.add(new CharacterRule(EnglishCharacterData.Alphabetical, Integer.parseInt(ruleValue)));
                } else if (ruleName.equals("Digit")) {
                    characterRules.add(new CharacterRule(EnglishCharacterData.Digit, Integer.parseInt(ruleValue)));
                } else {
                    logger.warning("Unknown character rule: " + ruleName);
                }
            } else {
                logger.warning("Invalid character rule: " + rule);
            }
        }
        return characterRules;
    }
    
        public static List<String> getErrorMessages(String password, List<CharacterRule> characterRules) {
            List<String> errorMessages = new ArrayList<>();
            if (password == null || password.isEmpty()) {
                errorMessages.add(BundleUtil.getStringFromBundle("password.validator.error.empty"));
            } else {
                if (password.length() < 8) {
                    errorMessages.add(BundleUtil.getStringFromBundle("password.validator.error.tooShort"));
                }
                if (characterRules.size() > 0) {
                    List<String> ruleErrors = getRuleErrors(password, characterRules);
                    errorMessages.addAll(ruleErrors);
                }
            }
            return errorMessages;
        }
    
        public static List<String> getRuleErrors(String password, List<CharacterRule> characterRules) {
            List<String> errorMessages = new ArrayList<>();
            for (CharacterRule rule : characterRules) {
                if (!rule.validate(password)) {
                    errorMessages.add       
 }

 

}