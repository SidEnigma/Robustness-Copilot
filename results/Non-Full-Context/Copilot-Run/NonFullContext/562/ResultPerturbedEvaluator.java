/* *********************************************************************** *
  * project: org.matsim.*
  * OutputDirectoryHierarchy.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 package org.matsim.core.controler;
 
 import java.io.File;
 
 import org.apache.log4j.Logger;
 import org.matsim.core.config.Config;
 import org.matsim.core.config.ConfigGroup;
 import org.matsim.core.config.groups.ControlerConfigGroup;
 import org.matsim.core.utils.io.IOUtils;
 
 import javax.inject.Inject;
 
 /**
  * 
  * Represents the directory hierarchy where the MATSim output goes in.
  * 
  * @author dgrether, michaz
  *
  */
 public final class OutputDirectoryHierarchy {
 
 	public enum OverwriteFileSetting {failIfDirectoryExists, overwriteExistingFiles, deleteDirectoryIfExists}
 
 	private static final String DIRECTORY_ITERS = "ITERS";
 	
 	private static Logger log = Logger.getLogger(OutputDirectoryHierarchy.class);
 	
 	private String runId = null;
 	
 	private final String outputPath;
 
 	private final ControlerConfigGroup.CompressionType defaultCompressionType;
 	
 	private OverwriteFileSetting overwriteFiles = OverwriteFileSetting.failIfDirectoryExists;
 
 	@Inject
 	OutputDirectoryHierarchy(ControlerConfigGroup config) {
 
 		this(config.getOutputDirectory(),
 				config.getRunId(),
 				config.getOverwriteFileSetting(),
 				config.getCompressionType());
 	}
 
 	/**
 	 * A constructor with a fairly powerful argument so that it can be adapted to functionality changes without having to change the API.
 	 *
 	 * @param config
 	 */
 	public OutputDirectoryHierarchy( Config config ) {
 		this( config.controler().getOutputDirectory(), config.controler().getRunId(), config.controler().getOverwriteFileSetting(), config.controler().getCompressionType() );
 	}
 
 	public OutputDirectoryHierarchy(String outputPath, OverwriteFileSetting overwriteFiles, ControlerConfigGroup.CompressionType defaultCompressionType) {
 		this(outputPath, null, overwriteFiles, true, defaultCompressionType);
 	}
 	
 	public OutputDirectoryHierarchy(String outputPath, String runId, OverwriteFileSetting overwriteFiles, ControlerConfigGroup.CompressionType defaultCompressionType) {
 		this(outputPath, runId, overwriteFiles, true, defaultCompressionType);
 	}	
 	/**
 	 * 
 	 * @param runId the runId, may be null
 	 * @param overwriteFiles overwrite existing files instead of crashing
 	 * @param outputPath the path to the output directory
 	 * @param createDirectories create the directories or abort if they exist
 	 */
 	public OutputDirectoryHierarchy(String outputPath, String runId, OverwriteFileSetting overwriteFiles, boolean createDirectories, ControlerConfigGroup.CompressionType compressionType){
 		this.overwriteFiles = overwriteFiles;
 		if (outputPath.endsWith("/")) {
 			outputPath = outputPath.substring(0, outputPath.length() - 1);
 		}
 		this.outputPath = outputPath;
 		this.runId = runId;
 		this.defaultCompressionType = compressionType;
 		if (createDirectories){
 			this.createDirectories();
 		}
 	}
 
 	/**
 	 * Returns the path to a directory where temporary files can be stored.
 	 *
 	 * @return path to a temp-directory.
 	 */
 	public final String getTempPath() {
 		return outputPath + "/tmp";
 	}
 
 	/**
 	 * Returns the path to the specified iteration directory. The directory path
 	 * does not include the trailing '/'.
 	 *
 	 * @param iteration
 	 *            the iteration the path to should be returned
 	 * @return path to the specified iteration directory
 	 */
 	public final String getIterationPath(final int iteration) {
 		return outputPath + "/" + DIRECTORY_ITERS + "/it." + iteration;
 	}
 
 	/**
 	 * Returns the complete filename to access an iteration-file with the given
 	 * basename.
 	 *
 	 * @param filename
 	 *            the basename of the file to access
 	 * @return complete path and filename to a file in a iteration directory. if rundId is set then it is prefixed with it
 	 */
 	public final String getIterationFilename(final int iteration, final String filename) {
 		StringBuilder s = new StringBuilder(getIterationPath(iteration));
 		s.append('/');
 		if (runId != null) {
 			s.append(runId);
 			s.append('.');
 		}
 		s.append(iteration);
 		s.append(".");
 		s.append(filename);
 		return s.toString();
 	}
 
 	public final String getIterationFilename(int iteration, Controler.DefaultFiles file) {
 		return getIterationFilename(iteration, file, this.defaultCompressionType);
 	}
 
 	public final String getIterationFilename(int iteration, Controler.DefaultFiles file, ControlerConfigGroup.CompressionType compression) {
 		if (compression == null) {
 			return getIterationFilename(iteration, file.filename);
 		}
 		return getIterationFilename(iteration, file.filename + compression.fileEnding);
 	}
 	
 	/**
 	 * Returns the complete filename to access a file in the output-directory.
 	 *
 	 * @param filename
 	 *            the basename of the file to access
 	 * @return complete path and filename to a file, if set prefixed with the runId,  in the output-directory
 	 */
 	public final String getOutputFilename(final String filename) {
 		StringBuilder s = new StringBuilder(outputPath);
 		s.append('/');
 		if (runId != null) {
 			s.append(runId);
 			s.append('.');
 		}
 		s.append(filename);
 		return s.toString();
 	}
 
 	public final String getOutputFilename(Controler.DefaultFiles file) {
 		return getOutputFilename(file, this.defaultCompressionType);
 	}
 
 	public final String getOutputFilename(Controler.DefaultFiles file, ControlerConfigGroup.CompressionType compression) {
 		if (compression == null) {
 			return getOutputFilename(Controler.OUTPUT_PREFIX + file.filename);
 		}
 		return getOutputFilename(Controler.OUTPUT_PREFIX + file.filename + compression.fileEnding);
 	}
 
 	
 	public String getOutputPath() {
 		return outputPath;
 	}
 
 
/** Defines a new dedicated path for all the iteration-related data. */
 public final void createIterationDirectory(final int iteration){
	 		createDirectory(getIterationPath(iteration));
 	}
 
 	/**
 	 * Creates a new directory in the output-directory.
 	 *
 	 * @param directory
 	 *            the name of the directory to create
 	 */
 	public final void createDirectory(final String directory) {
 		File f = new File(directory);
 		if (f.exists()) {
 			if (f.isDirectory()) {
 				return;
 			}
 			throw new RuntimeException("Cannot create directory " + directory + ": File exists and is not a directory.");
 		}
 		if (!f.mkdirs()) {
 			throw new RuntimeException("Cannot create directory " + directory + ": Creation failed.");
 		}
 	}
 
 	/**
 	 * Creates the output-directory and all its subdirectories.
 	 */
 	public final void createDirectories() {
 		createDirectory(outputPath);
 		createDirectory(outputPath + "/" + DIRECTORY_ITERS);
 		createDirectory(outputPath + "/" + DIRECTORY_ITERS + "/" + DIRECTORY_ITERS_TMP);
 		createDirectory(outputPath + "/" + DIRECTORY_ITERS + "/" + DIRECTORY_ITERS_TMP + "/" + DIRECTORY_ITERS_TMP_ITERS);
 		createDirectory(outputPath + "/" + DIRECTORY_ITERS + "/" + DIRECTORY_ITERS_TMP + "/" + DIRECTORY_ITERS_TMP_ITERS + "/" + DIRECTORY_ITERS_TMP_ITERS_ITERS);
 		createDirectory(outputPath + "/" + DIRECTORY_ITERS + "/" + DIRECTORY_ITERS_TMP + "/" + DIRECTORY_ITERS_TMP_ITERS + "/" + DIRECTORY_ITERS_TMP_ITERS_ITERS + "/" + DIRECTORY_ITERS_TMP_ITERS_IT		
 }

 

}