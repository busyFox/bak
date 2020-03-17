package com.gotogames.common.filemonitor;

import java.io.File;

public interface FileMonitorListener {
	public void fileChanged(File file);
}
