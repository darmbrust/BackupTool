package backup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.swt.widgets.Display;

import backup.config.BackupConfig;
import backup.config.BackupConfigStore;
import backup.gui.BackupTool;
import backup.gui.RunStatusViewer;
import backup.worker.Backup;
import backup.worker.Backup.Failure;

/**
 * This is the start point for the backup tool.  It will launch a gui for 
 * configuration, launch a gui for syncing, or run at the command line.
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

public class Launcher
{
	public static boolean configWindowOpen = false;

	public static boolean statusWindowOpen = false;

	public static void runGui(boolean config)
	{
		Display display = new Display();

		if (config)
		{
			new BackupTool(display);

		}
		else
		{
			new RunStatusViewer(display);
		}

		while (configWindowOpen || statusWindowOpen)
		{
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();

		System.exit(0);
	}

	public static void main(String[] args)
	{
		if (args.length == 1)
		{
			if (args[0].equals("-rg"))
			{
				runGui(false);
			}
			else if (args[0].equals("-rc"))
			{
				File file = BackupConfigStore.locateConfig();
				if (file == null)
				{
					System.out.println("Could not locate the configuration file");
					return;
				}

				ArrayList<BackupConfig> bc;
				try
				{
					bc = BackupConfigStore.readConfigs(file);
				}
				catch (Exception e)
				{
					System.out.println("Error reading config file");
					e.printStackTrace();
					return;
				}
                
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a ' on ' EEE MMM d");

				for (int i = 0; i < bc.size(); i++)
				{
					System.out.println();
                    System.out.println("Configuration was read from:" + file.getAbsolutePath());
					System.out.println("Running config " + (i + 1) + " out of " + bc.size());
                    System.out.println("Set started at: " + sdf.format(new Date(System.currentTimeMillis())));
					Backup backup = new Backup(bc.get(i));

					while (!backup.finishedCopying())
					{
						try
						{
							Thread.sleep(500);
						}
						catch (InterruptedException e)
						{
							// noop
						}
					}
                    System.out.println("Config finished at: " + sdf.format(new Date(System.currentTimeMillis())));
                    System.out.println("Files copied to "
                            + bc.get(i).getTargetFile().getAbsolutePath());
                    System.out.println("Files scanned: " + backup.getFileScanCount());
					System.out.println("Files copied:" + backup.getFileCopyCount());
                    System.out.println("Size of files copied: " + Utility.formatBytes(backup.getBytesCopied()));
					System.out.println("Obsolete files removed:" + backup.getObsoleteFileRemovedCount());
                    System.out.println("Files failed:" + backup.getFileFailureCount());

					Failure[] fail = backup.getFailures();
					if (fail.length > 0)
					{
                        System.out.println();
						System.out.println("Failures:");
						for (int j = 0; j < fail.length; j++)
						{
							System.out.println();
							System.out.println(fail[j].toString());
						}
					}

					String[] copied = backup.getCopiedFiles();

                    System.out.println();
					System.out.println("Copied:");
					for (int j = 0; j < copied.length; j++)
					{
						System.out.println(copied[j]);
					}
				}

			}
			else
			{
				System.out
						.println("Unknown parameter.  Supported parameters are '-rg' (run with gui) and 'rc' (run with no gui - command line output only).  No parameters will launch the configuration gui");
			}
		}
		else
		{
			runGui(true);
		}

	}
}
