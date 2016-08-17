package backup.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import backup.Launcher;
import backup.Utility;
import backup.config.BackupConfig;
import backup.config.BackupConfigStore;

/**
 * This is the GUI that allows you to choose what files you want to backup.
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

public class BackupTool
{
    private ArrayList<BackupConfig> backupConfigs = new ArrayList<BackupConfig>();

    private Combo                   backupSelection_;

    private Combo                   backupLocation_;
    
    private Combo                   topWrittenFolder_;

    private CheckStateListener      csl_;

    private CheckboxTreeViewer      tv_;

    private Button                  removeObsolete_;

    private Combo                   timeStampLeniancy_;

    private Combo                   copyThreads_;

    Shell                           shell_;

    private Text                    globalSkip_;

    public BackupTool(Display display)
    {
        Launcher.configWindowOpen = true;
        shell_ = new Shell(display);
        shell_.setLayout(new GridLayout(5, false));
        shell_.setText("Backup Configuration - " + Utility.getVersionNumber());
        shell_.setImage(new Image(display, this.getClass().getResourceAsStream("/icons/icon.gif")));

        shell_.addDisposeListener(new DisposeListener()
        {

            public void widgetDisposed(DisposeEvent arg0)
            {
                Launcher.configWindowOpen = false;
            }
        });

        makeLabel(shell_, "Choose the location to create the backup:", GridData.BEGINNING, 4);
        Link l = makeLink(shell_, "<a>about</a>", GridData.END, 1);
        l.addSelectionListener(new SelectionListener()
		{
		
			public void widgetSelected(SelectionEvent arg0)
			{
				MessageBox mb = new MessageBox(shell_, SWT.OK);
		        mb.setText("About Backup Tool - " + Utility.getVersionNumber());
		        mb.setMessage("Backup Tool Version " + Utility.getVersionNumber() + "\n"
						+ "Copyright 2010, Daniel Armbrust All Rights Reserved\n"
						+ "This code is licensed under the Apache License - v 2.0. \n"
						+ "A full copy of the license has been included with the distribution.\n"
						+ "E-Mail me at daniel.armbrust@gmail.com.\n"
						+ "or visit http://armbrust.webhop.net/");
		        mb.open();
			}
		
			public void widgetDefaultSelected(SelectionEvent arg0)
			{
				// noop
			}
		});
        backupLocation_ = new Combo(shell_, SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 5;
        backupLocation_.setLayoutData(gd);
        backupLocation_.setToolTipText("The folder where the backup should be created");
        backupLocation_.setItems(new String[]{"", "Browse..."});
        backupLocation_.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent arg0)
            {
                if (backupLocation_.getSelectionIndex() == backupLocation_.getItemCount() - 1)
                {
                    DirectoryDialog dd = new DirectoryDialog(shell_);
                    String result = dd.open();

                    if (result != null && result.length() > 0)
                    {
                        backupLocation_.setItem(0, result);
                        getCurrentBackupConfig().setTargetFile(new File(result));
                    }
                   	backupLocation_.select(0);
                }
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop
            }

        });
        
        makeLabel(shell_, "Optionally choose a virtual drive root to use when writing to the backup location:", GridData.BEGINNING, 5);
        topWrittenFolder_ = new Combo(shell_, SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 5;
        topWrittenFolder_.setLayoutData(gd);
        topWrittenFolder_.setToolTipText("Optional - By default, all folders to the drive root are written to the backup folder.  When selected, only folders below this folder are created in the backup location.");
        topWrittenFolder_.setItems(new String[]{"<Default>", "Browse..."});
        topWrittenFolder_.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent arg0)
            {
                if (topWrittenFolder_.getSelectionIndex() == topWrittenFolder_.getItemCount() - 1)
                {
                    DirectoryDialog dd = new DirectoryDialog(shell_);
                    String result = dd.open();

                    if (result != null && result.length() > 0)
                    {
                    	if (topWrittenFolder_.getItemCount() == 3)
                    	{
                    		topWrittenFolder_.setItem(1, result);
                    	}
                    	else  //must be two
                    	{
                    		topWrittenFolder_.add(result, 1);
                    	}
                    	topWrittenFolder_.select(1);
                    }
                    else
                    {
                    	topWrittenFolder_.select(0);
                    }
                }
                if (topWrittenFolder_.getItemCount() == 2 || topWrittenFolder_.getSelectionIndex() == 0)
                {
                	getCurrentBackupConfig().setParentOfTopWrittenFolder(null);
                }
                else
                {
               		getCurrentBackupConfig().setParentOfTopWrittenFolder(new File(topWrittenFolder_.getItem(1)));	
                }
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop
            }

        });

        removeObsolete_ = new Button(shell_, SWT.CHECK);
        removeObsolete_.setText("Remove Obsolete Files");
        removeObsolete_
                .setToolTipText("If you select this option - any file that exists in the backup location but does not exist in the source selection will be removed from the backup location");
        removeObsolete_.setSelection(true);
        removeObsolete_.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));
        removeObsolete_.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent arg0)
            {
                getCurrentBackupConfig().setRemoveObsoleteFiles(removeObsolete_.getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop

            }

        });

        Label tsl = makeLabel(shell_, "Time Stamp Leniancy", SWT.NONE, 1);
        tsl
                .setToolTipText("The time difference that is allowed between when comparing files before deciding that they are different");
        timeStampLeniancy_ = new Combo(shell_, SWT.READ_ONLY);
        timeStampLeniancy_
                .setToolTipText("The time difference that is allowed between when comparing files before deciding that they are different");
        timeStampLeniancy_.setItems(new String[]{"0", "5", "10", "100", "1000"});
        timeStampLeniancy_.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
        timeStampLeniancy_.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent arg0)
            {
                getCurrentBackupConfig().setTimeStampLeniancy(Integer.parseInt(timeStampLeniancy_.getText()));
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop

            }

        });

        Label tc = makeLabel(shell_, "Simultaneous copy operations", SWT.NONE, 1);
        tc
                .setToolTipText("The number of files to copy at a time.  \nIf you are backing up to a local disk, 1 is recommended.  \nIf you are backing up to a remote location, a higher number is recommended.\nThis only affect the speed of the copy.");
        tc.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        copyThreads_ = new Combo(shell_, SWT.READ_ONLY);
        copyThreads_
                .setToolTipText("The number of files to copy at a time.  \nIf you are backing up to a local disk, 1 is recommended.  \nIf you are backing up to a remote location, a higher number is recommended.\nThis only affect the speed of the copy.");
        copyThreads_.setItems(new String[]{"1", "2", "3", "5", "10"});
        copyThreads_.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent arg0)
            {
                getCurrentBackupConfig().setCopyThreads(Integer.parseInt(copyThreads_.getText()));
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop

            }

        });

        makeLabel(shell_, "Choose the files to backup and the files to skip:", GridData.BEGINNING, 4);
        tv_ = new CheckboxTreeViewer(shell_);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 5;
        gd.heightHint = 250;
        tv_.getTree().setLayoutData(gd);
        tv_.setContentProvider(new FileTreeContentProvider());
        tv_.setLabelProvider(new FileTreeLabelProvider(shell_.getDisplay()));
        tv_.setInput("root"); // pass a non-null that will be ignored
        tv_
                .getTree()
                .setToolTipText(
                                "Check the files and folders that you would like to backup.  \nChecking a folder will cause the folder and all of its contents to be backed up.  \nIf you check a file or folder under a folder which is already checked - then this file or folder (and all subfolders) will be ignored.");

        csl_ = new CheckStateListener(this, tv_);
        tv_.addCheckStateListener(csl_);

        Composite lower = makeLowerSection(shell_);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 5;
        lower.setLayoutData(gd);

        shell_.pack();
        shell_.open();

        try
        {
            File configFile = BackupConfigStore.locateConfig();

            if (configFile == null)
            {
                MessageDialog
                        .openError(shell_, "Configuration read error",
                                   "Could not create the necessary folder structure to store your configuration.  This is bad!");

            }
            else
            {
                if (configFile.exists())
                {
                    backupConfigs = BackupConfigStore.readConfigs(configFile);
                }
                else
                {
                    MessageDialog
                            .openInformation(shell_, "No configuration file found",
                                             "No configuration file was found.  This is expected if this is the first time you have run the program.");

                }
            }
        }
        catch (Exception e)
        {
            MessageDialog
                    .openError(shell_, "Configuration read error",
                               "An unexpected error happened while reading the configuration file.  It is corrupt or unreadable.");
        }

        tv_.getTree().forceFocus();

        if (backupConfigs.size() == 0)
        {
            populateBackupConfigList(1, 0);
            backupConfigs.add(new BackupConfig());
        }
        else
        {
            populateBackupConfigList(backupConfigs.size(), 0);
        }

        updateForBackupSelection();
    }

    private Composite makeLowerSection(Shell shell)
    {
        Composite lower = new Composite(shell, SWT.None);
        GridLayout gl = new GridLayout(3, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        lower.setLayout(gl);

        makeLabel(lower, "Files to skip at any level", GridData.HORIZONTAL_ALIGN_BEGINNING, 1);

        globalSkip_ = makeText(lower, 2);
        globalSkip_
                .setToolTipText("Comma seperated list of file names or file * prefixed extensions to skip.  For example: 'file1, *.lock'");
        globalSkip_.addFocusListener(new FocusListener()
        {

            public void focusLost(FocusEvent arg0)
            {
                getCurrentBackupConfig().setGlobalFilesToSkip(globalSkip_.getText());
            }

            public void focusGained(FocusEvent arg0)
            {
                // noop
            }

        });

        backupSelection_ = new Combo(lower, SWT.READ_ONLY);
        GridData gd = new GridData(GridData.CENTER);
        backupSelection_.setLayoutData(gd);
        backupConfigs.add(new BackupConfig());
        populateBackupConfigList(backupConfigs.size(), 0);
        backupSelection_.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent arg0)
            {
                if (backupSelection_.getSelectionIndex() == backupSelection_.getItemCount() - 1)
                {
                    backupConfigs.add(new BackupConfig());
                    String newItem = "Backup Configuration " + backupConfigs.size();
                    backupSelection_.add(newItem, backupSelection_.getItemCount() - 1);
                    backupSelection_.select(backupSelection_.getItemCount() - 2);

                }
                updateForBackupSelection();
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop
            }

        });
        backupSelection_.select(0);

        Button remove = makeButton(lower, "Remove Configuration", GridData.CENTER);
        remove.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent arg0)
            {
                int i = backupSelection_.getSelectionIndex();
                if (backupSelection_.getItemCount() > 2)
                {
                    backupConfigs.remove(i);
                    populateBackupConfigList(backupConfigs.size(), 0);

                }
                else
                {
                    backupConfigs.set(0, new BackupConfig());
                }

                updateForBackupSelection();

            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop

            }

        });

        Button run = makeButton(lower, "Run Backup Now", GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END);
        run.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent arg0)
            {
                for (int i = 0; i < backupConfigs.size(); i++)
                {
                	String validation = backupConfigs.get(i).validate();
                    if (validation != null)
                    {
                        backupSelection_.select(i);
                        updateForBackupSelection();

                        MessageDialog.openError(backupSelection_.getShell(), "Validation Error",
                                                validation);

                        return;
                    }
                }

                if (!saveConfig())
                {
                    MessageDialog.openError(backupSelection_.getShell(), "Configuration Save Error",
                                            "Could not save the configuration.");
                    return;
                }

                new CopySummary(shell_, BackupTool.this);
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop

            }

        });

        return lower;
    }

    private void populateBackupConfigList(int count, int selectItem)
    {
        String[] temp = new String[count + 1];
        for (int j = 0; j < temp.length - 1; j++)
        {
            temp[j] = "Backup Configuration " + (j + 1);
        }
        temp[temp.length - 1] = "Create new configuration...";
        backupSelection_.setItems(temp);
        backupSelection_.select(selectItem);

    }

    private Label makeLabel(Composite parent, String label, int gridLayoutParam, int hSpan)
    {
        Label l = new Label(parent, SWT.NONE);
        l.setText(label);
        GridData gd = new GridData(gridLayoutParam);
        gd.horizontalSpan = hSpan;
        l.setLayoutData(gd);
        return l;
    }
    
    private Link makeLink(Composite parent, String text, int gridLayoutParam, int hSpan)
    {
        Link l = new Link(parent, SWT.NONE);
        l.setText(text);
        GridData gd = new GridData(gridLayoutParam);
        gd.horizontalSpan = hSpan;
        l.setLayoutData(gd);
        return l;
    }

    private Button makeButton(Composite parent, String text, int gridData)
    {
        Button b = new Button(parent, SWT.PUSH);
        b.setText(text);
        b.setLayoutData(new GridData(gridData));
        return b;
    }

    public static Text makeText(Composite parent, int hSpan)
    {
        return makeText(parent, "", hSpan);
    }

    public static Text makeText(Composite parent, String toolTipText, int hSpan)
    {
        Text temp = new Text(parent, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = hSpan;
        temp.setLayoutData(gd);
        temp.setToolTipText(toolTipText);
        return temp;
    }

    protected void update(ArrayList<File> filesToBackup, ArrayList<File> filesToSkip)
    {
        getCurrentBackupConfig().setFilesToSkip(filesToSkip);
        getCurrentBackupConfig().setSourceFiles(filesToBackup);
    }
    
    private BackupConfig getCurrentBackupConfig()
    {
        return backupConfigs.get(backupSelection_.getSelectionIndex());
    }

    private void updateForBackupSelection()
    {
        BackupConfig bc = getCurrentBackupConfig();
        backupLocation_.setItem(0, bc.getTargetFile() == null ? ""
                : bc.getTargetFile().getAbsolutePath());
        backupLocation_.select(0);
        
        if (bc.getParentOfTopWrittenFolder() == null)
        {
        	topWrittenFolder_.select(0);
        	if (topWrittenFolder_.getItemCount() > 2)
        	{
        		topWrittenFolder_.remove(1);
        	}
        }
        else
        {
        	if (topWrittenFolder_.getItemCount() > 2)
        	{
        		topWrittenFolder_.setItem(1, bc.getParentOfTopWrittenFolder().getAbsolutePath());
        	}
        	else
        	{
        		topWrittenFolder_.add(bc.getParentOfTopWrittenFolder().getAbsolutePath(), 1);
        	}
        	topWrittenFolder_.select(1);
        }

        tv_.setInput("nothing");
        ArrayList<String> warnings = csl_.restoreSelections(bc.getSourceFiles(), bc.getFilesToSkip());

        if (warnings.size() > 0)
        {
            StringBuffer temp = new StringBuffer();
            for (int i = 0; i < warnings.size(); i++)
            {
                temp.append("\n" + warnings.get(i));
            }
            MessageDialog.openWarning(removeObsolete_.getShell(), "Configuration Restore Error",
                                      "Some of the files that you had previously chosen to backup no longer exist.\n"
                                              + "Here are the files that no longer exist:\n"
                                              + temp.toString());
        }

        removeObsolete_.setSelection(bc.getRemoveObsoleteFiles());
        timeStampLeniancy_.setText(bc.getTimeStampLeniancy() + "");
        copyThreads_.setText(bc.getCopyThreads() + "");
        ArrayList<String> gs = bc.getGlobalFilesToSkip();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < gs.size(); i++)
        {
            sb.append(gs.get(i));
            if (i + 1 < gs.size())
            {
                sb.append(", ");
            }
        }
        globalSkip_.setText(sb.toString());

    }

    protected void startBackup()
    {
        new RunStatusViewer(backupSelection_.getDisplay());
        Launcher.configWindowOpen = false;
        backupSelection_.getShell().setVisible(false);
        backupSelection_.getShell().dispose();
    }

    private boolean saveConfig()
    {
        File configFile = BackupConfigStore.locateConfig();

        if (configFile == null)
        {
            return false;
        }

        try
        {
            BackupConfigStore.writeConfigs(configFile, backupConfigs);
        }
        catch (IOException e)
        {
            return false;
        }
        return true;

    }

}
