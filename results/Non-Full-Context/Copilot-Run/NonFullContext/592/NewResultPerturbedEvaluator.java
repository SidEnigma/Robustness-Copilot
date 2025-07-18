/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2019 Adobe
  * %%
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * #L%
  */
 package com.adobe.acs.commons.util;
 
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 
 import javax.servlet.ServletOutputStream;
 import javax.servlet.ServletResponse;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Helper class to be used in {@link ServletResponse} wrappers.
  * It allows to buffer the output without committing it to the underlying response.
  * Also it exposes methods to access the buffers for the writer and output stream.
  * When calling close it will automatically spool the buffers to the underlying response.
  */
 public final class BufferedServletOutput {
 
     private static final Logger log = LoggerFactory.getLogger(BufferedServletResponse.class);
 
     public enum ResponseWriteMethod {
         OUTPUTSTREAM, WRITER
     }
 
     private final ServletResponse wrappedResponse;
     private final StringWriter writer;
     private final PrintWriter printWriter;
     private final ByteArrayOutputStream outputStream;
     private final ServletOutputStream servletOutputStream;
     private boolean flushWrappedBuffer;
     private ResponseWriteMethod writeMethod;
     private boolean flushBufferOnClose = true;
 
     /** 
      * Creates a new servlet output buffering both the underlying writer and output stream.
      * @param wrappedResponse the wrapped response
      */
     public BufferedServletOutput(ServletResponse wrappedResponse) {
         this(wrappedResponse, new StringWriter(), new ByteArrayOutputStream());
     }
 
     /** Creates a new servlet output using the given StringWriter and OutputStream as buffers.
      * 
      * @param wrappedResponse the wrapped response
      * @param writer          the writer to use as buffer (may be {@code null} in case you don't want to buffer the writer)
      * @param outputStream    the {@link ByteArrayOutputStream} to use as buffer for getOutputStream() (may be {@code null} in case
      *                            you don't want to buffer the output stream)
      */
     public BufferedServletOutput(ServletResponse wrappedResponse, StringWriter writer, ByteArrayOutputStream outputStream) {
         this.wrappedResponse = wrappedResponse;
         this.writer = writer;
         if (writer != null) {
             this.printWriter = new PrintWriter(writer);
         } else {
             this.printWriter = null;
         }
         this.outputStream = outputStream;
         if (outputStream != null) {
             this.servletOutputStream = new ServletOutputStreamWrapper(outputStream);
         } else {
             this.servletOutputStream = null;
         }
     }
 
     ServletOutputStream getOutputStream() throws IOException {
         if (ResponseWriteMethod.WRITER.equals(this.writeMethod)) {
             throw new IllegalStateException("Cannot invoke getOutputStream() once getWriter() has been called.");
         }
         this.writeMethod = ResponseWriteMethod.OUTPUTSTREAM;
         if (servletOutputStream != null) {
             return servletOutputStream;
         } else {
             return wrappedResponse.getOutputStream();
         }
     }
 
     PrintWriter getWriter() throws IOException {
         if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
             throw new IllegalStateException("Cannot invoke getWriter() once getOutputStream() has been called.");
         }
         this.writeMethod = ResponseWriteMethod.WRITER;
         if (printWriter != null) {
             return printWriter;
         } else {
             return wrappedResponse.getWriter();
         }
     }
 
     /**
      * @return {@link ResponseWriteMethod#OUTPUTSTREAM} in case {@link #getOutputStream()} has been called,
      *         {@link ResponseWriteMethod#WRITER} in case {@link #getWriter()} has been called, {@code null} in case none of those have been
      *         called yet. */
     public ResponseWriteMethod getWriteMethod() {
         return writeMethod;
     }
 
     /**
      * 
      * @return the buffered string which is the content of the response being written via {@link #getWriter()}
      * @throws IllegalStateException in case {@link #getWriter()} has not been called yet or the writer was not buffered.
      */
     public String getBufferedString() {
         if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
             throw new IllegalStateException("Cannot invoke getBufferedString() once getOutputStream() has been called.");
         }
         if (writer == null) {
             throw new IllegalStateException("Cannot get buffered string, as the writer was not buffered!");
         }
         return writer.toString();
     }
     
     /**
      * Finds if there's still data pending, which needs to be flushed. Could be implemented
      * with "getBufferedString().length() > 0, but that throws exceptions we don't like here.
      * @return true if there is data pending in this buffer
      */
     boolean hasPendingData() {
         if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
             return false;
         }
         if (writer == null) {
             return false;
         }
         return writer.toString().length() > 0;
     }
 
     /**
      * 
      * @return the buffered bytes which which were written via {@link #getOutputStream()}
      * @throws IllegalStateException in case {@link #getOutputStream()} has not been called yet or the output stream was not buffered.
      */
     public byte[] getBufferedBytes() {
         if (ResponseWriteMethod.WRITER.equals(this.writeMethod)) {
             throw new IllegalStateException("Cannot invoke getBufferedBytes() once getWriter() has been called.");
         }
         if (outputStream == null) {
             throw new IllegalStateException("Cannot get buffered bytes, as the output stream was not buffered!");
         }
         return outputStream.toByteArray();
     }
 
     /**
      * Flushes the buffers bound to this object. In addition calls {@link ServletResponse#flushBuffer()} of the underlying response.
      */
     public void resetBuffer() {
         if (writer != null) {
             writer.getBuffer().setLength(0);
         }
         if (outputStream != null) {
             outputStream.reset();
         }
         wrappedResponse.resetBuffer();
     }
 
     /** 
      * Influences the behavior of the buffered data during calling {@link #close()}.
      * If {@code flushBufferOnClose} is {@code true} (default setting) the buffer is flushed to the wrapped response, otherwise the buffer is discarded.
      * @param flushBufferOnClose
      */
     public void setFlushBufferOnClose(boolean flushBufferOnClose) {
         this.flushBufferOnClose = flushBufferOnClose;
     }
 
 
/** Checks whether flushBufferOnClose is set to true. If that's the case, it closes the buffered output stream or the writer to the underlying/wrapped response. */

public void close() throws IOException {
    if (flushBufferOnClose) {
        if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
            if (servletOutputStream != null) {
                servletOutputStream.close();
            }
        } else {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }
}
 

}