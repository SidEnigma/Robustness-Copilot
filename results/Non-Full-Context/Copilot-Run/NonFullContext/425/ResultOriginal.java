package tech.tablesaw.io;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.ibm.icu.text.CharsetDetector;
 import com.ibm.icu.text.CharsetMatch;
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringReader;
 import java.net.URL;
 import java.nio.charset.Charset;
 import java.util.Scanner;
 
 public class Source {
 
   // we always have one of these (file, reader, or inputStream)
   protected final File file;
   protected final Reader reader;
   protected final InputStream inputStream;
   protected final Charset charset;
 
   public Source(File file) {
     this(file, getCharSet(file));
   }
 
   public Source(File file, Charset charset) {
     this.file = file;
     this.reader = null;
     this.inputStream = null;
     this.charset = charset;
   }
 
   public Source(InputStreamReader reader) {
     this.file = null;
     this.reader = reader;
     this.inputStream = null;
     this.charset = Charset.forName(reader.getEncoding());
   }
 
   public Source(Reader reader) {
     this.file = null;
     this.reader = reader;
     this.inputStream = null;
     this.charset = null;
   }
 
   public Source(InputStream inputStream) {
     this(inputStream, Charset.defaultCharset());
   }
 
   public Source(InputStream inputStream, Charset charset) {
     this.file = null;
     this.reader = null;
     this.inputStream = inputStream;
     this.charset = charset;
   }
 
   public static Source fromString(String s) {
     return new Source(new StringReader(s));
   }
 
   public static Source fromUrl(String url) throws IOException {
     return new Source(new StringReader(loadUrl(url)));
   }
 
   public File file() {
     return file;
   }
 
   public Reader reader() {
     return reader;
   }
 
   public InputStream inputStream() {
     return inputStream;
   }
 
   public Charset getCharset() {
     return charset;
   }
 
 
/** If cachedBytes are not null, returns a Reader created from the cachedBytes. */
 public Reader createReader(byte[] cachedBytes) throws IOException{
    if (cachedBytes != null) {
      return new InputStreamReader(new ByteArrayInputStream(cachedBytes), charset);
    } else {
      return new InputStreamReader(inputStream, charset);
    }
  }
  
    public static String loadUrl(String url) throws IOException {
      Scanner scanner = new Scanner(new URL(url).openStream());
      scanner.useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    }
  
    public static Charset getCharSet(File file) {
      CharsetDetector detector = new CharsetDetector();
      detector.setText(file);
      CharsetMatch match = detector.detect();
      return match.getName();
    }
  
    @VisibleForTesting
    public static Source fromString(String s, Charset charset) {
      return new Source(new StringReader(s), charset);
    }
  
    @VisibleForTesting
    public static Source fromString(String s) {
      return new Source(new StringReader(s));
    }
  
    @VisibleForTesting
    public static Source fromUrl(String url, Charset charset) throws IOException {
      return new Source(new StringReader(loadUrl(url)), charset);
    }
  
    @VisibleForTesting
    public static Source fromUrl(String url) throws IOException {
      return new Source(new StringReader(loadUrl(url)));
    }
  
    @VisibleForTesting
    public static Source fromFile(File file) {
      return new Source(file);
    }
  
    @VisibleForTesting
    public static Source fromFile(File file, Charset charset) {
      return new Source(file, charset);
    }
  
    @VisibleForTesting
    public static Source fromInputStream(InputStream inputStream) {
      return new Source(inputStream);
    }
  
    @VisibleForTesting
    public static Source fromInputStream(InputStream inputStream, Charset charset   
 }

 

}