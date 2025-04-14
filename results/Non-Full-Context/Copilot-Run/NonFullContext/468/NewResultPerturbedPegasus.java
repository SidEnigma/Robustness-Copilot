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

public static List<CharacterRule> parseConfigString(String configString) {
    List<CharacterRule> characterRules = new ArrayList<>();

    if (StringUtils.isBlank(configString)) {
        return characterRules;
    }

    String[] ruleStrings = configString.split(",");
    for (String ruleString : ruleStrings) {
        String[] ruleParts = ruleString.split(":");
        if (ruleParts.length != 2) {
            logger.warning("Invalid rule format: " + ruleString);
            continue;
        }

        String ruleName = ruleParts[0].trim();
        String ruleValue = ruleParts[1].trim();

        EnglishCharacterData characterData = getCharacterData(ruleName);
        if (characterData == null) {
            logger.warning("Invalid rule name: " + ruleName);
            continue;
        }

        int ruleCount;
        try {
            ruleCount = Integer.parseInt(ruleValue);
        } catch (NumberFormatException e) {
            logger.warning("Invalid rule value: " + ruleValue);
            continue;
        }

        characterRules.add(new CharacterRule(characterData, ruleCount));
    }

    return characterRules;
}
 

}