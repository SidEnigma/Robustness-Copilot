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
 
 
/** Taking into account the formatting of the configString ("UpperCase:1,LowerCase:1,Digit:1,Special:1") parses the list of characters that are defined in the database. */
 public static List<CharacterRule> parseConfigString(String configString){
        List<CharacterRule> characterRules = new ArrayList<>();
        String[] configArray = configString.split(",");
        for (String config : configArray) {
            String[] configSplit = config.split(":");
            String characterType = configSplit[0];
            int minCount = Integer.parseInt(configSplit[1]);
            if (characterType.equals("UpperCase")) {
                characterRules.add(new CharacterRule(EnglishCharacterData.UpperCase, minCount));
            } else if (characterType.equals("LowerCase")) {
                characterRules.add(new CharacterRule(EnglishCharacterData.LowerCase, minCount));
            } else if (characterType.equals("Digit")) {
                characterRules.add(new CharacterRule(EnglishCharacterData.Digit, minCount));
            } else if (characterType.equals("Special")) {
                characterRules.add(new CharacterRule(EnglishCharacterData.Special, minCount));
            } else {
                logger.warning("Unknown character type: " + characterType);
            }
        }
        return characterRules;
    }
    
        public static List<String> getErrorMessages(String password, List<CharacterRule> characterRules) {
            List<String> errorMessages = new ArrayList<>();
            if (password.length() < 8) {
                errorMessages.add(BundleUtil.getStringFromBundle("password.tooShort"));
            }
            if (password.length() > 100) {
                errorMessages.add(BundleUtil.getStringFromBundle("password.tooLong"));
            }
            if (password.length() >= 8 && password.length() <= 100) {
                for (CharacterRule characterRule : characterRules) {
                    if (characterRule.getNumberOfCharacters(password) < characterRule.getMinCount()) {
                        errorMessages.add(BundleUtil.getStringFromBundle("password.insufficientCharacteristics"));
                        break;
                    }
                }
            }
            if (password.equals("password")) {
                errorMessages.add(B     
 }

 

}