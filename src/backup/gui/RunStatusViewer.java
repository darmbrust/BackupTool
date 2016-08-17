package backup.gui;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import backup.Launcher;
import backup.Utility;
import backup.config.BackupConfig;
import backup.config.BackupConfigStore;
import backup.worker.Backup;
import backup.worker.Backup.Failure;

/**
 * This is the GUI that displays (and drives) the backup process.
 * It shows the output / errors / etc.
 * 
 * <pre>
 *  Copyright (c) 2010  Daniel Armbrust.  All Rights Reserved.
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  The license was included with the download.
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * </pre>
 * @author <A HREF="mailto:daniel.armbrust@gmail.com">Dan Armbrust</A>
 */

public class RunStatusViewer
{
	private Backup currentBackup_;

	ArrayList<BackupConfig> backupConfigs_;

	private StyledText text_;

	private Shell shell_;

	private boolean stop = false;
	
	String configFile_;

	public RunStatusViewer(Display display)
	{
		Launcher.statusWindowOpen = true;
	
		shell_ = new Shell(display);
		shell_.setLayout(new GridLayout(1, false));
		shell_.setSize(640, 480);
		shell_.setText("Backup Status - " + Utility.getVersionNumber());
		shell_.setImage(new Image(display, this.getClass().getResourceAsStream("/icons/icon.gif")));

		text_ = new StyledText(shell_, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
		text_.setLayoutData(new GridData(GridData.FILL_BOTH));

		shell_.setVisible(true);
		shell_.forceFocus();
		
		try
		{
			File configFile = BackupConfigStore.locateConfig();
			
			if (configFile == null)
			{
				MessageDialog
				.openError(shell_, "Configuration read error",
						"An unexpected error happened while reading the configuration file.  It is corrupt or unreadable.");
				System.exit(0);

			}
			else
			{
				backupConfigs_ = BackupConfigStore.readConfigs(configFile);
				configFile_ = configFile.getAbsolutePath();
			}
		}
		catch (Exception e)
		{
			MessageDialog
					.openError(shell_, "Configuration read error",
							"An unexpected error happened while reading the configuration file.  It is corrupt or unreadable.");
			System.exit(0);
		}
		
		final Thread thread = new Thread(new Monitor());
		thread.start();

		shell_.addDisposeListener(new DisposeListener()
		{

			public void widgetDisposed(DisposeEvent arg0)
			{
				stop = true;
				thread.interrupt();
				Launcher.statusWindowOpen = false;

			}

		});
	}

	private class Monitor implements Runnable
	{
		
		private String summary = "Backup Finished.  You may close this window now." 
			+ "\nStarted at: " + Utility.getCurrentTime()
			+ "\n\nConfiguration was read from:\n" + configFile_;
        
        private Color red = shell_.getDisplay().getSystemColor(SWT.COLOR_RED);
        private Color white = shell_.getDisplay().getSystemColor(SWT.COLOR_WHITE);

		private int pos;
		
		public void run()
		{
			final StringBuffer temp = new StringBuffer();
			final ArrayList<StyleRange> styleRanges = new ArrayList<StyleRange>();
			
			
			for (int j = 0; j < backupConfigs_.size(); j++)
			{
				currentBackup_ = new Backup(backupConfigs_.get(j));

				temp.append("Configuration was read from:\n" + configFile_);
				temp.append("\nOn set " + (j + 1) + " of " + backupConfigs_.size() + " copying files to "
						+ backupConfigs_.get(j).getTargetFile().getAbsolutePath());
				temp.append("\nSet started at: " + Utility.getCurrentTime() + "\n");
				
				shell_.getDisplay().syncExec(new Runnable()
				{
					public void run()
					{
						text_.setText(temp.toString());
						pos = temp.length();
						temp.setLength(0);
					}
				});
				
				while (!currentBackup_.finishedCopying())
				{
					if (currentBackup_.getFinishedScanning())
					{
						temp.append("\nScanning finished - scanned: " + currentBackup_.getFileScanCount());
					}
					else
					{
						temp.append("\nScanning for files that need to be copied...");
						temp.append("\nMost recently scaned folder: " + currentBackup_.getCurrentScanningFolder());
					}
					temp.append("\nFiles checked so far: " + currentBackup_.getFileScanCount());
					temp.append("\nFiles left to copy: " + currentBackup_.getFilesRemainingCount());
					temp.append("\nFiles copied so far: " + currentBackup_.getFileCopyCount());
					temp.append("\nCopied so far: " + Utility.formatBytes(currentBackup_.getBytesCopied()));
					temp.append("\nObsolete files removed so far: "
							+ currentBackup_.getObsoleteFileRemovedCount());
					temp.append("\nFiles that failed to copy so far: "
							+ currentBackup_.getFileFailureCount());
					String last = currentBackup_.getLastCopiedFile();
					temp.append("\nMost recently copied file: "
							+ (last == null ? "" : last));
					
					ArrayList<String> filesBeingCopied = currentBackup_.getFilesBeingCopied();
					temp.append("\nFiles currently being copied: ");
					for (String t : filesBeingCopied)
					{
						temp.append("\n" + t);
					}

					shell_.getDisplay().syncExec(new Runnable()
					{
						public void run()
						{
							text_.replaceTextRange(pos, (text_.getText().length() - pos), temp.toString());
						}
					});
					temp.setLength(0);

					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
						if (stop)
						{
							return;
						}
					}
				}

				// final summary...

				temp.append("\n\nConfig finished at: " + Utility.getCurrentTime());
				temp.append("\nFiles copied to "
						+ backupConfigs_.get(j).getTargetFile().getAbsolutePath());
				if (backupConfigs_.get(j).getParentOfTopWrittenFolder() != null)
	            {
	            	temp.append("\nusing the virtual folder root:" + backupConfigs_.get(j).getParentOfTopWrittenFolder().getAbsolutePath());
	            }
				
                temp.append("\n\nFiles scanned: " + currentBackup_.getFileScanCount());
				temp.append("\nFiles copied: " + currentBackup_.getFileCopyCount());
				temp.append("\nSize of files copied: " + Utility.formatBytes(currentBackup_.getBytesCopied()));
				temp.append("\nObsolete files removed: "
						+ currentBackup_.getObsoleteFileRemovedCount());
				temp.append("\nFiles that failed to copy: " + currentBackup_.getFileFailureCount());

                //color the failures red.
                
				Failure[] fail = currentBackup_.getFailures();
				if (fail.length > 0)
				{
                    int redStart = summary.length() + temp.length();
                    
                    
					temp.append("\n\nFailures:");
					for (int i = 0; i < fail.length; i++)
					{
						temp.append("\n\n" + fail[i].toString());
					}
                    
                    int redLength = summary.length() + temp.length() - redStart;
                    styleRanges.add(new StyleRange(redStart, redLength, red, white));
				}

				temp.append("\n\nCopied Files:");
				String[] files = currentBackup_.getCopiedFiles();
				for (int i = 0; i < files.length; i++)
				{
					temp.append("\n" + files[i]);
				}

				summary += temp.toString();

				temp.setLength(0);
			}

			shell_.getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					text_.setText(summary);
                    text_.setStyleRanges(styleRanges.toArray(new StyleRange[styleRanges.size()]));
				}
			});
		}

	}
}
