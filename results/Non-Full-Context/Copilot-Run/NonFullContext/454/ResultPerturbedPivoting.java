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
 
 	/**
 	 * This function takes a path and tries to find the file in the file system or
 	 * in the resource path. The order of resolution is as follows:
 	 * 
 	 * <ol>
 	 * <li>Find path in file system</li>
 	 * <li>Find path in file system with compression extension (e.g. *.gz)</li>
 	 * <li>Find path in class path as resource</li>
 	 * <li>Find path in class path with compression extension</li>
 	 * </ol>
 	 *
 	 * In case the filename is a URL (i.e. starting with "file:" or "jar:file:"),
 	 * then no resolution is done but the provided filename returned as URL.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static URL resolveFileOrResource(String filename) throws UncheckedIOException {
 		try {
 			// I) do not handle URLs
 			if (filename.startsWith("jar:file:") || filename.startsWith("file:") || filename.startsWith( "https:" )) {
 				// looks like an URI
 				return new URL(filename);
 			}
 
 			// II) Replace home identifier
 			if (filename.startsWith("~" + File.separator)) {
 				filename = System.getProperty("user.home") + filename.substring(1);
 			}
 
 			// III.1) First, try to find the file in the file system
 			File file = new File(filename);
 
 			if (file.exists()) {
 				logger.info(String.format("Resolved %s to %s", filename, file));
 				return file.toURI().toURL();
 			}
 
 			// III.2) Try to find file with an additional postfix for compression
 			for (String postfix : COMPRESSION_EXTENSIONS.keySet()) {
 				file = new File(filename + "." + postfix);
 
 				if (file.exists()) {
 					logger.info(String.format("Resolved %s to %s", filename, file));
 					return file.toURI().toURL();
 				}
 			}
 
 			// IV.1) First, try to find the file in the class path
 			URL resource = IOUtils.class.getClassLoader().getResource(filename);
 
 			if (resource != null) {
 				logger.info(String.format("Resolved %s to %s", filename, resource));
 				return resource;
 			}
 
 			// IV.2) Second, try to find the resource with a compression extension
 			for (String postfix : COMPRESSION_EXTENSIONS.keySet()) {
 				resource = IOUtils.class.getClassLoader().getResource(filename + "." + postfix);
 
 				if (resource != null) {
 					logger.info(String.format("Resolved %s to %s", filename, resource));
 					return resource;
 				}
 			}
 
 			throw new FileNotFoundException(filename);
 		} catch (FileNotFoundException | MalformedURLException e) {
 			throw new UncheckedIOException(e);
 		}
 	}
 
 	/**
 	 * Gets the compression of a certain URL by file extension. May return null if
 	 * not compression is assumed.
 	 */
 	private static CompressionType getCompression(URL url) {
 
 		// .enc extension is ignored
 		String[] segments = url.getPath().replace(".enc", "").split("\\.");
 		String lastExtension = segments[segments.length - 1];
 		return COMPRESSION_EXTENSIONS.get(lastExtension.toLowerCase(Locale.ROOT));
 	}
 
 	/**
 	 * Opens an input stream for a given URL. If the URL has a compression
 	 * extension, the method will try to open the compressed file using the proper
 	 * decompression algorithm.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static InputStream getInputStream(URL url) throws UncheckedIOException {
 		try {
 			InputStream inputStream = url.openStream();
 
 			if (url.getPath().endsWith(".enc"))
 				inputStream = CipherUtils.getDecryptedInput(inputStream);
 
 			CompressionType compression = getCompression(url);
 			if (compression != null) {
 				switch (compression) {
 					case GZIP:
 						inputStream = new GZIPInputStream(inputStream);
 						break;
 					case LZ4:
 						inputStream = new LZ4FrameInputStream(inputStream);
 						break;
 					case BZIP2:
 						inputStream = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BZIP2, inputStream);
 						break;
 					case ZSTD:
 						inputStream = new ZstdInputStream(inputStream);
 						break;
 				}
 			}
 
 			return new UnicodeInputStream(new BufferedInputStream(inputStream));
 		} catch (IOException | CompressorException | GeneralSecurityException e) {
 			throw new UncheckedIOException(e);
 		}
 	}
 
 	/**
 	 * Creates a reader for an input URL. If the URL has a compression extension,
 	 * the method will try to open the compressed file using the proper
 	 * decompression algorithm. A given character set is used for the reader.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static BufferedReader getBufferedReader(URL url, Charset charset) throws UncheckedIOException {
 		InputStream inputStream = getInputStream(url);
 		return new BufferedReader(new InputStreamReader(inputStream, charset));
 	}
 
 	/**
 	 * See {@link #getBufferedReader(URL, Charset)}. UTF-8 is assumed as the
 	 * character set.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static BufferedReader getBufferedReader(URL url) throws UncheckedIOException {
 		return getBufferedReader(url, CHARSET_UTF8);
 	}
 
 	/**
 	 * Opens an output stream for a given URL. If the URL has a compression
 	 * extension, the method will try to open the compressed file using the proper
 	 * decompression algorithm. Note that compressed files cannot be appended and
 	 * that it is only possible to write to the file system (i.e. file:// protocol).
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	@SuppressWarnings("resource")
 	public static OutputStream getOutputStream(URL url, boolean append) throws UncheckedIOException {
 		try {
 			if (!url.getProtocol().equals("file")) {
 				throw new UncheckedIOException("Can only write to file:// protocol URLs");
 			}
 
 			File file = new File(url.toURI());
 			CompressionType compression = getCompression(url);
 
 			if ((compression != null && compression != CompressionType.ZSTD) && append && file.exists()) {
 				throw new UncheckedIOException("Cannot append to compressed files.");
 			}
 
 			OutputStream outputStream = new FileOutputStream(file, append);
 
 			if (compression != null) {
 				switch (compression) {
 					case GZIP:
 						outputStream = new GZIPOutputStream(outputStream);
 						break;
 					case LZ4:
 						outputStream = new LZ4FrameOutputStream(outputStream);
 						break;
 					case BZIP2:
 						outputStream = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, outputStream);
 						break;
 					case ZSTD:
 						outputStream = new ZstdOutputStream(outputStream, 6);
 						break;
 				}
 			}
 
 			return new BufferedOutputStream(outputStream);
 		} catch (IOException | CompressorException | URISyntaxException e) {
 			throw new UncheckedIOException(e);
 		}
 	}
 
 	/**
 	 * Creates a writer for an output URL. If the URL has a compression extension,
 	 * the method will try to open the compressed file using the proper
 	 * decompression algorithm. Note that compressed files cannot be appended and
 	 * that it is only possible to write to the file system (i.e. file:// protocol).
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static BufferedWriter getBufferedWriter(URL url, Charset charset, boolean append)
 			throws UncheckedIOException {
 		OutputStream outputStream = getOutputStream(url, append);
 		return new BufferedWriter(new OutputStreamWriter(outputStream, charset));
 	}
 
 	/**
 	 * See {@link #getBufferedWriter(URL, Charset, boolean)}. UTF-8 is assumed as
 	 * the character set and non-appending mode is used.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static BufferedWriter getBufferedWriter(URL url) throws UncheckedIOException {
 		return getBufferedWriter(url, CHARSET_UTF8, false);
 	}
 
 	/**
 	 * Wrapper function for {@link #getBufferedWriter(URL)} that creates a
 	 * PrintStream.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static PrintStream getPrintStream(URL url) throws UncheckedIOException {
 		return new PrintStream(getOutputStream(url, false));
 	}
 
 
/** Copies content from one stream to another. */
 public static void copyStream(final InputStream fromStream, final OutputStream toStream) throws UncheckedIOException{
	 		try {
 			final byte[] buffer = new byte[BUFFER_SIZE];
 			int bytesRead;
 			while ((bytesRead = fromStream.read(buffer)) != -1) {
 				toStream.write(buffer, 0, bytesRead);
 			}
 		} catch (IOException e) {
 			throw new UncheckedIOException(e);
 		}
 	}
 
 	/**
 	 * Copies content from one stream to another.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static void copyStream(final InputStream fromStream, final OutputStream toStream, final long maxBytes)
 			throws UncheckedIOException {
 		try {
 			final byte[] buffer = new byte[BUFFER_SIZE];
 			int bytesRead;
 			long bytesWritten = 0;
 			while ((bytesRead = fromStream.read(buffer)) != -1 && bytesWritten < maxBytes) {
 				toStream.write(buffer, 0, bytesRead);
 				bytesWritten += bytesRead;
 			}
 		} catch (IOException e) {
 			throw new UncheckedIOException(e);
 		}
 	}
 
 	/**
 	 * Copies content from one stream to another.
 	 * 
 	 * @throws UncheckedIOException
 	 */
 	public static void copyStream(final InputStream fromStream, final OutputStream toStream, final long maxBytes,
 			final long maxMillis) throws UncheckedIOException {
 		try {
 			final byte[] buffer = new byte[BUFFER_SIZE];
 			int bytesRead;
 			long bytesWritten = 0;
 			long startTime = System.currentTimeMillis();
 			while ((bytesRead = fromStream.read(buffer)) != -1 && bytesWritten <		
 }

 

}