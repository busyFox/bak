package com.gotogames.common.filemonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to monitor changes on file
 * File can be  file or directory and file can exist or not
 * @author pascal
 *
 */
public class FileMonitor {
	
	private HashMap<File, Long> mapFileModifiedTime = null;
	private Timer timer;
	private Collection<FileMonitorListener> listeners;
	
	public FileMonitor(long interval) {
		mapFileModifiedTime = new HashMap<File, Long>();
		listeners = new ArrayList<FileMonitorListener>();
		timer = new Timer(true);
		timer.schedule(new FileDirectoryMonitorNotifier(), 0, interval);
	}
	
	public void stop() {
		timer.cancel();
	}
	
	/**
	 * Add file to the monitored file list
	 * @param file
	 */
	public void addDirectory(File file) {
		if (!mapFileModifiedTime.containsKey(file)) {
			mapFileModifiedTime.put(file, (file.exists()?file.lastModified():-1));
		}
	}
	
	/**
	 * Remove file from monitored file list
	 * @param file
	 */
	public void removeDirectory(File file) {
		mapFileModifiedTime.remove(file);
	}
	
	/**
	 * Add listener to the file monitor
	 * @param listener
	 */
	public void addListener(FileMonitorListener listener) {
		for (Iterator<FileMonitorListener> iterator = listeners.iterator(); iterator.hasNext();) {
			FileMonitorListener temp = iterator.next();
			if (temp.equals(listener)) {
				return;
			}
		}
		listeners.add(listener);
	}
	
	/**
	 * remove listener from the file monitor
	 * @param listener
	 */
	public void removeListener(FileMonitorListener listener) {
		for (Iterator<FileMonitorListener> iterator = listeners.iterator(); iterator.hasNext();) {
			FileMonitorListener temp = iterator.next();
			if (temp.equals(listener)) {
				iterator.remove();
				break;
			}
		}
	}
	
	/**
	 * Task executed every n millisecond
	 * @author pascal
	 *
	 */
	private class FileDirectoryMonitorNotifier extends TimerTask {

		@Override
		public void run() {
			ArrayList<File> listFiles = new ArrayList<File>(mapFileModifiedTime.keySet());
			for (Iterator<File> iterator = listFiles.iterator(); iterator.hasNext();) {
				File file = iterator.next();
				// time stored
				long lastModifiedTime = mapFileModifiedTime.get(file);
				// current time
				long currentModifiedTime = file.exists() ? file.lastModified() : -1;
				
				// file has changed ?
				if (lastModifiedTime != currentModifiedTime) {
					// record new time for this file
					mapFileModifiedTime.put(file, currentModifiedTime);
					
					// notify listeners
					for (Iterator<FileMonitorListener> iterator2 = listeners.iterator(); iterator2.hasNext();) {
						FileMonitorListener listener = iterator2.next();
						if (listener == null) {
							iterator2.remove();
						} else {
							listener.fileChanged(file);
						}
						
					}
				}
			}
		}
		
	}
}
