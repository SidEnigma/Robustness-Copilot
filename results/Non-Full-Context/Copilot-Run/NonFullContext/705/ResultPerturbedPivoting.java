/* *********************************************************************** *
  * project: org.matsim.*
  * EventWriterXML.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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
 
 package org.matsim.core.events.algorithms;
 
 import org.matsim.api.core.v01.events.Event;
 import org.matsim.core.events.handler.BasicEventHandler;
 import org.matsim.core.utils.io.IOUtils;
 import org.matsim.core.utils.io.UncheckedIOException;
 
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.nio.charset.StandardCharsets;
 import java.util.Map;
 
 public class EventWriterXML implements EventWriter, BasicEventHandler {
 	private final BufferedWriter out;
 
 	public EventWriterXML(final String outfilename) {
 		this.out = IOUtils.getBufferedWriter(outfilename);
 		this.writeHeader();
 	}
 
 	/**
 	 * Constructor so you can pass System.out or System.err to the writer to see the result on the console.
 	 *
 	 * @param stream
 	 */
 	public EventWriterXML(final OutputStream stream ) {
 		this.out = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
 		this.writeHeader();
 	}
 
 	private void writeHeader() {
 		try {
 			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
 		} catch (IOException e) {
 			throw new UncheckedIOException(e);
 		}
 	}
 
 	@Override
 	public void closeFile() {
 		try {
 			this.out.write("</events>");
 			// I added a "\n" to make it look nicer on the console.  Can't say if this may have unintended side
 			// effects anywhere else.  kai, oct'12
 			// fails signalsystems test (and presumably other tests in contrib/playground) since they compare
 			// checksums of event files.  Removed that change again.  kai, oct'12
 			this.out.close();
 		} catch (IOException e) {
 			throw new UncheckedIOException(e);
 		}
 	}
 
 	@Override
 	public void reset(final int iter) {
 	}
 
 	@Override
 	public void handleEvent(final Event event) {
 		try {
 			this.out.append("\t<event ");
 			Map<String, String> attr = event.getAttributes();
 			for (Map.Entry<String, String> entry : attr.entrySet()) {
 				this.out.append(entry.getKey());
 				this.out.append("=\"");
 				this.out.append(encodeAttributeValue(entry.getValue()));
 				this.out.append("\" ");
 			}
 			this.out.append(" />\n");
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}
 
 	// the following method was taken from MatsimXmlWriter in order to correctly encode attributes, but
 	// to forego the overhead of using the full MatsimXmlWriter.
 
/** Encodes the given string so that it no longer contains characters that have a special meaning in xml. */
 private String encodeAttributeValue(final String attributeValue){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < attributeValue.length(); i++) {
			char c = attributeValue.charAt(i);
			if (c == '<') {
				sb.append("&lt;");
			} else if (c == '>') {
				sb.append("&gt;");
			} else if (c == '&') {
				sb.append("&amp;");
			} else if (c == '"') {
				sb.append("&quot;");
			} else if (c == '\'') {
				sb.append("&apos;");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}		
 }

 

}