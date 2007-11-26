package org.zlibrary.core.xml;

import java.io.*;
import java.util.Map;

import org.zlibrary.core.library.ZLibrary;

public abstract class ZLXMLReader {
	public boolean read(String fileName) {
		InputStream stream = ZLibrary.getInstance().getResourceInputStream(fileName);
		if (stream == null) {
			try {
				stream = new BufferedInputStream(new FileInputStream(fileName));
			} catch (FileNotFoundException e) {
			}
		}
		return (stream != null) ? ZLXMLProcessorFactory.getInstance().createXMLProcessor().read(this, stream) : false;
	}
	
	public void startElementHandler(String tag, Map<String, String> attributes) {
		
	}
	
	public void endElementHandler(String tag) {
		
	}
	
	public void characterDataHandler(char[] ch, int start, int length) {
		
	}

	//?
	public void startDocumentHandler() {
		
	}
	
	public void endDocumentHandler() {
		
	}
	
}
