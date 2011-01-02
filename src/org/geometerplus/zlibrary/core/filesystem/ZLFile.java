/*
 * Copyright (C) 2007-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.core.filesystem;

import java.io.*;
import java.util.*;

public abstract class ZLFile {
	private final static HashMap<String,ZLFile> ourCachedFiles = new HashMap<String,ZLFile>();

	protected interface ArchiveType {
		int	NONE = 0;
		int	GZIP = 0x0001;
		int	BZIP2 = 0x0002;
		int	COMPRESSED = 0x00ff;
		int	ZIP = 0x0100;
		int	TAR = 0x0200;
		int	ARCHIVE = 0xff00;
	};
	
	private String myExtension;
	private String myShortName;
	protected int myArchiveType;
	private boolean myIsCached;
	protected void init() {
		final String name = getLongName();
		final int index = name.lastIndexOf('.');
		myExtension = (index > 0) ? name.substring(index + 1).toLowerCase().intern() : "";
		myShortName = name.substring(name.lastIndexOf('/') + 1);

		/*
		if (lowerCaseName.endsWith(".gz")) {
			myNameWithoutExtension = myNameWithoutExtension.substring(0, myNameWithoutExtension.length() - 3);
			lowerCaseName = lowerCaseName.substring(0, lowerCaseName.length() - 3);
			myArchiveType = myArchiveType | ArchiveType.GZIP;
		}
		if (lowerCaseName.endsWith(".bz2")) {
			myNameWithoutExtension = myNameWithoutExtension.substring(0, myNameWithoutExtension.length() - 4);
			lowerCaseName = lowerCaseName.substring(0, lowerCaseName.length() - 4);
			myArchiveType = myArchiveType | ArchiveType.BZIP2;
		}
		*/
		int archiveType = ArchiveType.NONE;
		if (myExtension == "zip") {
			archiveType |= ArchiveType.ZIP;
		} else if (myExtension == "oebzip") {
			archiveType |= ArchiveType.ZIP;
		} else if (myExtension == "epub") {
			archiveType |= ArchiveType.ZIP;
		} else if (myExtension == "tar") {
			archiveType |= ArchiveType.TAR;
		//} else if (lowerCaseName.endsWith(".tgz")) {
			//nothing to-do myNameWithoutExtension = myNameWithoutExtension.substr(0, myNameWithoutExtension.length() - 3) + "tar";
			//myArchiveType = myArchiveType | ArchiveType.TAR | ArchiveType.GZIP;
		}
		myArchiveType = archiveType;
	}
	
	public static ZLFile createFile(ZLFile parent, String name) {
		ZLFile file = null;
		if (parent == null) {
			ZLFile cached = ourCachedFiles.get(name);
			if (cached != null) {
				return cached;
			}
			if (!name.startsWith("/")) {
				return ZLResourceFile.createResourceFile(name);
			} else {
				return new ZLPhysicalFile(name);
			}
		} else if ((parent instanceof ZLPhysicalFile) && (parent.getParent() == null)) {
			// parent is a directory
			file = new ZLPhysicalFile(parent.getPath() + '/' + name);
		} else if (parent instanceof ZLResourceFile) {
			file = ZLResourceFile.createResourceFile((ZLResourceFile)parent, name);
		} else {
			file = ZLArchiveEntryFile.createArchiveEntryFile(parent, name);
		}

		if (!ourCachedFiles.isEmpty() && (file != null)) {
			ZLFile cached = ourCachedFiles.get(file.getPath());
			if (cached != null) {
				return cached;
			}
		}
		return file;
	}

	public static ZLFile createFileByPath(String path) {
		if (path == null) {
			return null;
		}
		ZLFile cached = ourCachedFiles.get(path);
		if (cached != null) {
			return cached;
		}

		if (!path.startsWith("/")) {
			return ZLResourceFile.createResourceFile(path);
		}
		int index = path.lastIndexOf(':');
		if (index > 1) {
			return ZLArchiveEntryFile.createArchiveEntryFile(
				createFileByPath(path.substring(0, index)), path.substring(index + 1)
			);
		}
		return new ZLPhysicalFile(path);
	}

	public abstract long size();
	public abstract boolean exists();
	public abstract boolean isDirectory();
	public abstract String getPath();
	public abstract ZLFile getParent();
	public abstract ZLPhysicalFile getPhysicalFile();
	public abstract InputStream getInputStream() throws IOException;

	public boolean isReadable() {
		return true;
	}

	public final boolean isCompressed() {
		return (0 != (myArchiveType & ArchiveType.COMPRESSED)); 
	}
	
	public final boolean isArchive() {
		return (0 != (myArchiveType & ArchiveType.ARCHIVE));
	}

	public abstract String getLongName();

	public final String getShortName() {
		return myShortName;
	}

	public final String getExtension() {
		return myExtension;
	}

	protected List<ZLFile> directoryEntries() {
		return Collections.emptyList();
	}

	public final List<ZLFile> children() {
		if (exists()) {
			if (isDirectory()) {
				return directoryEntries();
			} else if (isArchive()) {
				return ZLArchiveEntryFile.archiveEntries(this);
			}
		}
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		return getPath().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ZLFile)) {
			return false;
		}
		return getPath().equals(((ZLFile)o).getPath());
	}

	protected boolean isCached() {
		return myIsCached;
	}

	public void setCached(boolean cached) {
		myIsCached = cached;
		if (cached) {
			ourCachedFiles.put(getPath(), this);
		} else {
			ourCachedFiles.remove(getPath());
			if (0 != (myArchiveType & ArchiveType.ZIP)) {
				ZLZipEntryFile.removeFromCache(this);
			}
		}
	}
}
