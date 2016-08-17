package backup.gui;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

import backup.Utility;
import backup.config.BackupConfig;
import backup.config.BackupConfigStore;

/**
 * Display a summary of the files that will be copied.
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

public class CopySummary
{
    public CopySummary(Shell parent, final BackupTool bt)
    {
        final Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.RESIZE);
        shell.setLayout(new GridLayout(5, false));
        shell.setText("Backup Summary - " + Utility.getVersionNumber());
        shell.setImage(new Image(shell.getDisplay(), CopySummary.class.getResourceAsStream("/icons/icon.gif")));
        shell.setSize(600, 400);

        shell.setLayout(new GridLayout(2, true));

        StyledText st = new StyledText(shell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        st.setLayoutData(gd);

        boolean happy = populate(st);

        Button ok = new Button(shell, SWT.PUSH);
        ok.setText("Ok");
        if (!happy)
        {
            ok.setEnabled(false);
        }
        ok.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        ok.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent arg0)
            {
                bt.startBackup();
                shell.dispose();
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop
            }

        });

        Button cancel = new Button(shell, SWT.PUSH);
        cancel.setText("Cancel");
        cancel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        cancel.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent arg0)
            {
                shell.dispose();
            }

            public void widgetDefaultSelected(SelectionEvent arg0)
            {
                // noop
            }

        });

        shell.open();
        ok.setSize(cancel.getSize());

    }

    private boolean populate(StyledText st)
    {
        File configFile = BackupConfigStore.locateConfig();
        if (configFile == null || !configFile.exists())
        {
            st.append("The configuration file could not be found.");
            return false;
        }

        ArrayList<BackupConfig> backupConfigs;
        try
        {
            backupConfigs = BackupConfigStore.readConfigs(configFile);
        }

        catch (Exception e)
        {
            st.append("Their was an error reading the config file.");
            return false;
        }

        st.append("The configuration file was read from:\n" + configFile.getAbsolutePath());

        for (int i = 0; i < backupConfigs.size(); i++)
        {
            st.append("\n\nBackup Configuration " + (i + 1));
            st.append("\n\nFiles will be backed up to:" + backupConfigs.get(i).getTargetFile().getAbsolutePath());
            if (backupConfigs.get(i).getParentOfTopWrittenFolder() != null)
            {
            	st.append("\n\nThe virtual folder root is:" + backupConfigs.get(i).getParentOfTopWrittenFolder().getAbsolutePath());
            }
            st.append("\n\nFiles to be backed up:");
            ArrayList<File> sources = backupConfigs.get(i).getSourceFiles();
            for (int j = 0; j < sources.size(); j++)
            {
                st.append("\n" + sources.get(j).getAbsolutePath());
            }

            st.append("\n\nGlobal files to skip:");
            ArrayList<String> globalSkip = backupConfigs.get(i).getGlobalFilesToSkip();
            for (int j = 0; j < globalSkip.size(); j++)
            {
                st.append("\n" + globalSkip.get(j));
            }
            
            st.append("\n\nFiles to be skipped:");
            ArrayList<File> skip = backupConfigs.get(i).getFilesToSkip();
            for (int j = 0; j < skip.size(); j++)
            {
                st.append("\n" + skip.get(j).getAbsolutePath());
            }
        }

        return true;
    }

}
