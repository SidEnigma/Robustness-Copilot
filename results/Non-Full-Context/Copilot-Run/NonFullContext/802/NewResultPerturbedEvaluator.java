/* *********************************************************************** *
  * project: org.matsim.*
  * IOUtils.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 
 package org.matsim.core.utils.io;
 
 import com.github.luben.zstd.ZstdInputStream;
 import com.github.luben.zstd.ZstdOutputStream;
 import net.jpountz.lz4.LZ4FrameInputStream;
 import net.jpountz.lz4.LZ4FrameOutputStream;
 import org.apache.commons.compress.compressors.CompressorException;
 import org.apache.commons.compress.compressors.CompressorStreamFactory;
 import org.apache.log4j.Logger;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.DataInputStream;
 import java.io.EOFException;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintStream;
 import java.net.MalformedURLException;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.nio.charset.Charset;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.FileVisitResult;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.SimpleFileVisitor;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.security.GeneralSecurityException;
 import java.util.Arrays;
 import java.util.Locale;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.zip.GZIPInputStream;
 import java.util.zip.GZIPOutputStream;
 
 /**
  * This class provides helper methods for input/output in MATSim.
  * 
  * The whole I/O infrastructure is based on URLs, which allows more flexibility
  * than String-based paths or URLs. The structure follows three levels: Stream
  * level, writer/reader level, and convenience methods.
  * 
  * <h2>Stream level</h2>
  * 
  * The two main methods on the stream level are {@link #getInputStream(URL)} and
  * {@link #getOutputStream(URL, boolean)}. Their use is rather obvious, the
  * boolean argument of the output stream is whether it is an appending output
  * stream. Depending on the extension of the reference file of the URL,
  * compression will be detected automatically. See below for a list of active
  * compression algorithms.
  * 
  * <h2>Reader/Writer level</h2>
  * 
  * Use {@link #getBufferedWriter(URL, Charset, boolean)} and its simplified
  * versions to obtained a BufferedWriter object. Use
  * {@link #getBufferedReader(URL)} to obtain a BufferedReader. These functions
  * should be used preferredly, because they allow for future movements of files
  * to servers etc.
  * 
  * <h2>Convenience methods</h2>
  * 
  * Two convenience methods exist: {@link #getBufferedReader(String)} and
  * {@link #getBufferedReader(String)}, which take a String-based path as input.
  * They intentionally do not allow for much flexibility (e.g. choosing the
  * character set of the files). If this is needed, please use the reader/writer
  * level methods and construct the URL via the helper functions that are
  * documented below.
  * 
  * <h2>URL handling</h2>
  * 
  * To convert a file name to a URL, use {@link #getFileUrl(String)}. This is
  * mostly useful to determine the URL for an output file. If you are working
  * with input files, the best is to make use of
  * {@link #resolveFileOrResource(String)}, which will first try to find a
  * certain file in the file system and then in the class path (i.e. in the Java
  * resources). This makes it easy to write versatile code that can work with
  * local files and resources at the same time.
  * 
  * <h2>Compression</h2>
  * 
  * Compressed files are automatically assumed if certain file types are
  * encountered. Currently, the following patterns match certain compression
  * algorithms:
  * 
  * <ul>
  * <li><code>*.gz</code>: GZIP compression</li>
  * <li><code>*.lz4</code>: LZ4 compression</li>
  * <li><code>*.bz2</code>: Bzip2 compression</li>
  * <li><code>*.zst</code>: ZStandard compression</li>
  * </ul>
  *
  * <h2>Encryption</h2>
  *
  * Files ending with {@code .enc} are assumed to be encrypted and will be handled with {@link CipherUtils}.
  *
  */
 final public class IOUtils {
 	/**
 	 * This is only a static helper class.
 	 */
 	private IOUtils() {
 	}
 
 	private enum CompressionType { GZIP, LZ4, BZIP2, ZSTD }
 
 	// Define compressions that can be used.
 	private static final Map<String, CompressionType> COMPRESSION_EXTENSIONS = new TreeMap<>();
 
 	static {
 		COMPRESSION_EXTENSIONS.put("gz", CompressionType.GZIP);
 		COMPRESSION_EXTENSIONS.put("lz4", CompressionType.LZ4);
 		COMPRESSION_EXTENSIONS.put("bz2", CompressionType.BZIP2);
 		COMPRESSION_EXTENSIONS.put("zst", CompressionType.ZSTD);
 	}
 
 	// Define a number of charsets that are / have been used.
 	public static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;
 	public static final Charset CHARSET_WINDOWS_ISO88591 = StandardCharsets.ISO_8859_1;
 
 	// We niw use Unix line endings everywhere.
 	public static final String NATIVE_NEWLINE = "\n";
 
 	// Logger
 	private final static Logger logger = Logger.getLogger(IOUtils.class);
 
 
/** This function tries to find in the given path if the file is in the file system or in the resource path */

public static URL resolveFileOrResource(String filename) throws UncheckedIOException {
    URL url = null;
    try {
        // Check if the file exists in the file system
        File file = new File(filename);
        if (file.exists()) {
            url = file.toURI().toURL();
        } else {
            // Check if the file exists in the resource path
            ClassLoader classLoader = IOUtils.class.getClassLoader();
            url = classLoader.getResource(filename);
        }
    } catch (MalformedURLException e) {
        throw new UncheckedIOException("Error resolving file or resource: " + filename, e);
    }
    return url;
}
 

}