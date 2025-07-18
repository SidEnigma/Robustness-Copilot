
 /* *********************************************************************** *
  * project: org.matsim.*
  * CommandLine.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2019 by the members listed in the COPYING,        *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * *********************************************************************** */
 
  package org.matsim.core.config;
 
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.stream.Collectors;
 
 import org.apache.log4j.Logger;
 import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
 
 /**
  * <p>
  * This class provides functionality to configure (MATSim) applications via the
  * command line. While it can be used as a tool for configuration of any Java
  * application in itself, it provides advanced functionality to directly modify
  * the MATSim {@link Config} object.
  * </p>
  * 
  * <h1>General usage</h1>
  * 
  * <p>
  * The command line interpreter is set up using the {@link CommandLine.Builder}:
  * </p>
  * 
  * <pre>
  * CommandLine cmd = new CommandLine.Builder(args) //
  * 		.allowOptions("optionA", "optionB") //
  * 		.requireOptions("outputPath") //
  * 		.allowPositionalArguments(false).build();
  * </pre>
  * 
  * <p>
  * The command line option can be accessed via safe getters, which return
  * {@link Optional}s, or strict getters, which raise exceptions:
  * </p>
  * 
  * <pre>
  * int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt).orElse(4);
  * int numberOfThreads = Integer.parseInt(cmd.getOptionStrict("threads"));
  * </pre>
  * 
  * <p>
  * As can be seen, options are always returned as strings. It is the task of the
  * user to convert the arguments to the expected data types. They are given in
  * one of the following ways:
  * </p>
  * 
  * <ul>
  * <li>Value following the option name: <code>--threads 20</code></li>
  * <li>With equals sign between: <code>--threads=20</code></li>
  * </ul>
  * 
  * <h2>MATSim usage</h2>
  * 
  * <p>
  * In order to configure MATSim with the command line, one needs to tell the
  * interpreter to apply the command line options to the MATSim {@link Config}:
  * </p>
  * 
  * <pre>
  * CommandLine cmd = new CommandLine.Builder(args) //
  * 		.allowPositionalArguments(false)//
  * 		.build();
  * 
  * Config config = ConfigUtils.createConfig();
  * cmd.applyConfiguration(config);
  * </pre>
  * 
  * <p>
  * This will interpret all command line options of the form
  * <code>config:*</code> as options that are supposed to be inserted into the
  * MATSim config. The rules are as follows:
  * </p>
  * 
  * <ul>
  * <li><code>--config:MODULE.PARAM VALUE</code> sets a certain parameter with
  * name <code>PARAM</code> in the module <code>MODULE</code> to the value
  * <code>VALUE</code>. The way this passed value is intepreted is up to the
  * MATSim {@link ConfigGroup}, just as if the value would have come from the
  * configuration file.</li>
  * <li><code>--config:MODULE.SET_TYPE[ID_PARAM=ID_VALUE].PARAM VALUE</code> sets
  * a value in a specific parameter set, which is identified using a specific
  * parameter in that set with a specific selection value.</li>
  * </ul>
  * 
  * Some examples:
  * 
  * <ul>
  * <li><code>--config:global.numberOfThreads 48</code></li>
  * <li><code>--config:strategy.strategysettings[strategyName=ReRoute].weight 0.0</code></li>
  * <li><code>--config:planCalcScore.scoringParameters[subpopulation=null].modeParams[mode=car].constant -3.5</code></li>
  * </ul>
  * 
  * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
  */
 public class CommandLine {
 	final private static Logger logger = Logger.getLogger(CommandLine.class);
 
 	final private static String CONFIG_PREFIX = "config";
 	final private static String FLAG_VALUE = "true";
 
 	final private Set<String> allowedPrefixes;
 	final private Set<String> allowedOptions;
 	final private Set<String> requiredOptions;
 
 	final private Map<String, String> options = new HashMap<>();
 	final private List<String> positionalArguments = new LinkedList<>();
 
 	final private boolean positionalArgumentsAllowed;
 	final private boolean allowAnyOption;
 
 	// Configuration part
 
 	/**
 	 * Fluent builder to create a command line interpreter for MATSim applications.
 	 */
 	static public class Builder {
 		final private Set<String> allowedPrefixes = new HashSet<>(Collections.singleton(CONFIG_PREFIX));
 		final private Set<String> allowedOptions = new HashSet<>();
 		final private Set<String> requiredOptions = new HashSet<>();
 
 		private boolean positionalArgumentsAllowed = true;
 		private boolean allowAnyOption = false;
 
 		final private List<String> arguments;
 
 		/**
 		 * Creates a builder instance.
 		 * 
 		 * @param args The command line arguments passed to the Java application.
 		 */
 		public Builder(String[] args) {
 			this.arguments = Arrays.asList(args);
 			for( String argument : this.arguments ){
 				if ( argument==null) {
 					throw new RuntimeException( "one of the entries in args is null; this will not work ..." ) ;
 				}
 			}
 		}
 
 		/**
 		 * Passing <code>false</code> to this function disables unnamed (positional)
 		 * command line arguments. The default is to allow them.
 		 */
 		public Builder allowPositionalArguments(boolean allow) {
 			positionalArgumentsAllowed = allow;
 			return this;
 		}
 
 		/**
 		 * Passing <code>true</code> to this function allows any named command line
 		 * option to be passed to the application. By default only those which have been
 		 * defined explicitly are accepted.
 		 */
 		public Builder allowAnyOption(boolean allow) {
 			allowAnyOption = allow;
 			return this;
 		}
 
 		/**
 		 * Adds a list of allowed command line options to the application. If not
 		 * configured otherwise (see {@link #allowAnyOption}), arguments that have not
 		 * been added explicitly will raise an error.
 		 */
 		public Builder allowOptions(Collection<String> options) {
 			allowedOptions.addAll(options);
 			return this;
 		}
 
 		/**
 		 * @see {@link #allowOptions(Collection)}
 		 */
 		public Builder allowOptions(String... options) {
 			allowOptions(Arrays.asList(options));
 			return this;
 		}
 
 		/**
 		 * Adds a list of required command line options. If those options are not
 		 * present an error will be raised.
 		 */
 		public Builder requireOptions(Collection<String> options) {
 			allowedOptions.addAll(options);
 			requiredOptions.addAll(options);
 			return this;
 		}
 
 		/**
 		 * @see {@link #requireOptions(Collection)}
 		 */
 		public Builder requireOptions(String... options) {
 			requireOptions(Arrays.asList(options));
 			return this;
 		}
 
 		/**
 		 * Allows a list of prefixes, which means that all command line options of the
 		 * form <code>PREFIX:*</code> can be used for the application. Alternatively,
 		 * the application can be configured to allow any named command line argument
 		 * (see {@link #allowAnyOption}).
 		 */
 		public Builder allowPrefixes(Collection<String> prefixes) {
 			allowedPrefixes.addAll(prefixes);
 			return this;
 		}
 
 		/**
 		 * @see {{@link #allowedPrefixes}
 		 */
 		public Builder allowPrefixes(String... prefixes) {
 			allowPrefixes(Arrays.asList(prefixes));
 			return this;
 		}
 
 		/**
 		 * Builds the command line interpreter with the given configuration. An
 		 * {@link ConfigurationException} may be thrown directly in one of the following
 		 * cases:
 		 * 
 		 * <ul>
 		 * <li>An unnamed (positional) command line argument is passed, although it is
 		 * forbidden (see {{@link #allowPositionalArguments(boolean)})</li>
 		 * <li>A named command line argument is passed, but it is not allowed (see
 		 * {@link #allowOptions(Collection)}. This can be disabled via
 		 * {@link #allowAnyOption}.</li>
 		 * <li>A named command line argument with a prefix is passed, but the prefix is
 		 * not allowed (see {@link #allowPrefixes(Collection)}. This can be disabled via
 		 * {@link #allowAnyOption}.</li>
 		 * <li>Some command line options have been defined as required (see
 		 * {@link #requireOptions(Collection)}), but are not present.</li>
 		 * </ul>
 		 * 
 		 * @throws ConfigurationException
 		 */
 		public CommandLine build() throws ConfigurationException {
 			CommandLine commandLine = new CommandLine(allowedOptions, requiredOptions, allowedPrefixes,
 					positionalArgumentsAllowed, allowAnyOption);
 			commandLine.process(arguments);
 			return commandLine;
 		}
 	}
 
 	private CommandLine(Set<String> allowedOptions, Set<String> requiredOptions, Set<String> allowedPrefixes,
 			boolean positionalArgumentsAllowed, boolean allowAnyOption) {
 		this.allowedOptions = allowedOptions;
 		this.requiredOptions = requiredOptions;
 		this.allowedPrefixes = allowedPrefixes;
 		this.positionalArgumentsAllowed = positionalArgumentsAllowed;
 		this.allowAnyOption = allowAnyOption;
 	}
 
 	// Getters for positional arguments
 
 	/**
 	 * Returns the number of positional (unnamed) arguments that have been passed to
 	 * the application.
 	 */
 	public int getNumberOfPositionalArguments() {
 		return positionalArguments.size();
 	}
 
 	/**
 	 * Returns the positional (unnamed) arguments that have been passed to the
 	 * application.
 	 */
 	public List<String> getPositionalArguments() {
 		return Collections.unmodifiableList(positionalArguments);
 	}
 
 	/**
 	 * Returns the positional (unnamed) command line argument with index
 	 * <code>index</code> if present.
 	 */
 	public Optional<String> getPositionalArgument(int index) {
 		return index < positionalArguments.size() ? Optional.of(positionalArguments.get(index)) : Optional.empty();
 	}
 
 	/**
 	 * Returns the positional (unnamed) command line argument with index
 	 * <code>index</code> if present or raises a {@link ConfigurationException}.
 	 *
 	 * @throws ConfigurationException
 	 */
 	public String getPositionalArgumentStrict(int index) throws ConfigurationException {
 		if (index < positionalArguments.size()) {
 			return positionalArguments.get(index);
 		} else {
 			throw new ConfigurationException(String.format(
 					"Requested positional command line argument with index %d, but only %d arguments are available",
 					index, positionalArguments.size()));
 		}
 	}
 
 	// Getters for options
 
 	/**
 	 * Returns a list of available named command line options.
 	 */
 	public Collection<String> getAvailableOptions() {
 		return Collections.unmodifiableCollection(options.keySet());
 	}
 
 	/**
 	 * Returns whether a certain named command line option is present.
 	 */
 	public boolean hasOption(String option) {
 		return options.containsKey(option);
 	}
 
 	/**
 	 * Returns a named command line option if it is present.
 	 */
 	public Optional<String> getOption(String option) {
 		return options.containsKey(option) ? Optional.of(options.get(option)) : Optional.empty();
 	}
 
 	/**
 	 * Returns a named command line option if it is present, raises a
 	 * {@link ConfigurationException} otherwise.
 	 * 
 	 * @throws ConfigurationException
 	 */
 	public String getOptionStrict(String option) throws ConfigurationException {
 		if (options.containsKey(option)) {
 			return options.get(option);
 		} else {
 			throw new ConfigurationException(
 					String.format("Requested command line option '%s' is not available", option));
 		}
 	}
 
 	// Processing
 
 	/**
 	 * Processes a list of raw command line arguments.
 	 * 
 	 * @throws ConfigurationException
 	 */
 	private void process(List<String> args) throws ConfigurationException {
 		List<String> arguments = flattenArguments(args);
 		positionalArguments.clear();
 
 		String currentOption = null;
 
 		for (String token : arguments) {
 			if (token.startsWith("--")) {
 				if (currentOption != null) {
 					addOption(currentOption, FLAG_VALUE);
 				}
 
 				currentOption = token.substring(2);
 			} else {
 				if (currentOption != null) {
 					addOption(currentOption, token);
 				} else {
 					addPositionalArgument(token);
 				}
 
 				currentOption = null;
 			}
 		}
 
 		if (currentOption != null) {
 			addOption(currentOption, FLAG_VALUE);
 		}
 
 		checkRequiredOptions();
 		reportOptions();
 	}
 
 	/**
 	 * Flattens a list of raw command line arguments:
 	 * 
 	 * <ul>
 	 * <li>Splits <code>["param=value"]</code> into <code>["param", "value"]</code>,
 	 * </li>
 	 * <li>but makes sure that <code>["param[key=value]"]</code> is preserved</li>
 	 * </ul>
 	 * 
 	 * @param args
 	 * @return
 	 */
 	private List<String> flattenArguments(List<String> args) {
 		List<String> flatArguments = new LinkedList<>();
 
 		for (String argument : args) {
 			int index = argument.lastIndexOf('=');
 			int bracketIndex = argument.lastIndexOf(']');
 
 			if (bracketIndex > index) {
 				index = argument.indexOf('=', bracketIndex);
 			}
 
 			if (index > -1) {
 				flatArguments.add(argument.substring(0, index));
 				flatArguments.add(argument.substring(index + 1));
 			} else {
 				flatArguments.add(argument);
 			}
 		}
 
 		return flatArguments;
 	}
 
 	/**
 	 * Adds a positional argument and checks if this is allowed by configuration.
 	 * 
 	 * @throws ConfigurationException
 	 */
 	private void addPositionalArgument(String value) throws ConfigurationException {
 		if (!positionalArgumentsAllowed) {
 			throw new ConfigurationException(String.format("Positional argument '%s' is not allowed.", value));
 		}
 
 		positionalArguments.add(value);
 	}
 
 	/**
 	 * Adds a named option and checks whether it is allowed by configuration.
 	 * 
 	 * @throws ConfigurationException
 	 */
 	private void addOption(String option, String value) throws ConfigurationException {
 		if (!allowAnyOption && !allowedOptions.contains(option)) {
 			String[] parts = option.split(":");
 
 			if (!allowedPrefixes.contains(parts[0])) {
 				throw new ConfigurationException(String.format("Option '%s' is not allowed.", option));
 			}
 		}
 
 		options.put(option, value);
 	}
 
 	/**
 	 * Checks that are required options are present.
 	 * 
 	 * @throws ConfigurationException
 	 */
 	private void checkRequiredOptions() throws ConfigurationException {
 		List<String> missingOptions = new LinkedList<>();
 
 		for (String option : requiredOptions) {
 			if (!options.containsKey(option)) {
 				missingOptions.add(option);
 			}
 		}
 
 		if (missingOptions.size() > 0) {
 			throw new ConfigurationException(
 					String.format("The following options are missing: %s", missingOptions.toString()));
 		}
 	}
 
 	// MATSim Configuration
 
 
/** Applies command-line configuration to a MATSim instance {@link Config}. */
 public void applyConfiguration(Config config) throws ConfigurationException{}

 

}