package backup.config;

import java.io.*;
import java.util.*;

/**
 * This a bean like class for storing the details of a backup configuration.
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

public class BackupConfig
{
    private ArrayList<String> globalFilesToSkip    = new ArrayList<String>();
    private ArrayList<File>   filesToSkip_         = new ArrayList<File>();
    private int               timeStampLeniancy_   = 10;
    private int               copyThreads          = 1;
    private boolean           removeObsoleteFiles_ = true;
    private ArrayList<File>   sourceFiles_         = new ArrayList<File>();
    private File              targetFile_;
    private File 			  parentOfTopWrittenFolder_;

    /**
     * @return the filesToSkip
     */
    public ArrayList<File> getFilesToSkip()
    {
        return this.filesToSkip_;
    }

    /**
     * @param filesToSkip the filesToSkip to set
     */
    public void setFilesToSkip(ArrayList<File> filesToSkip)
    {
        this.filesToSkip_ = filesToSkip;
    }

    /**
     * @return the removeObsoleteFiles
     */
    public boolean getRemoveObsoleteFiles()
    {
        return this.removeObsoleteFiles_;
    }

    /**
     * @param removeObsoleteFiles the removeObsoleteFiles to set
     */
    public void setRemoveObsoleteFiles(boolean removeObsoleteFiles)
    {
        this.removeObsoleteFiles_ = removeObsoleteFiles;
    }

    /**
     * @return the sourceFiles
     */
    public ArrayList<File> getSourceFiles()
    {
        return this.sourceFiles_;
    }

    /**
     * @param sourceFiles the sourceFiles to set
     */
    public void setSourceFiles(ArrayList<File> sourceFiles)
    {
        this.sourceFiles_ = sourceFiles;
    }

    /**
     * @return the targetFile
     */
    public File getTargetFile()
    {
        return this.targetFile_;
    }

    /**
     * @param targetFile the targetFile to set
     */
    public void setTargetFile(File targetFile)
    {
        this.targetFile_ = targetFile;
    }

    /**
     * @return the timeStampLeniancy
     */
    public int getTimeStampLeniancy()
    {
        return this.timeStampLeniancy_;
    }

    /**
     * @param timeStampLeniancy the timeStampLeniancy to set
     */
    public void setTimeStampLeniancy(int timeStampLeniancy)
    {
        this.timeStampLeniancy_ = timeStampLeniancy;
    }

    public int getCopyThreads()
    {
        return copyThreads;
    }

    public void setCopyThreads(int copyThreads)
    {
        this.copyThreads = copyThreads;
    }

    public ArrayList<String> getGlobalFilesToSkip()
    {
        return this.globalFilesToSkip;
    }

    public void setGlobalFilesToSkip(ArrayList<String> globalFilesToSkip)
    {
        this.globalFilesToSkip = globalFilesToSkip;
    }

    public void setGlobalFilesToSkip(String filesToSkip)
    {
        int start = 0;

        ArrayList<String> fts = new ArrayList<String>();
        if (filesToSkip != null)
        {
            int pos = filesToSkip.indexOf(',', start);

            while (pos > 0)
            {
                fts.add(filesToSkip.substring(start, pos).trim());
                start = pos + 1;
                if (start > filesToSkip.length())
                {
                    break;
                }
                pos = filesToSkip.indexOf(',', start);
            }
            fts.add(filesToSkip.substring(start).trim());
        }
        this.globalFilesToSkip = fts;
    }

	public File getParentOfTopWrittenFolder()
	{
		return this.parentOfTopWrittenFolder_;
	}

	public void setParentOfTopWrittenFolder(File topWrittenFolder)
	{
		this.parentOfTopWrittenFolder_ = topWrittenFolder;
	}
	
	/**
	 * Returns null if validation is successful, otherwise, an error message.
	 * @return
	 */
	public String validate()
	{
		if (getTargetFile() == null || !getTargetFile().isDirectory())
        {
            return "You must provide the target folder.";
        }
		
		else if (getParentOfTopWrittenFolder() != null && getParentOfTopWrittenFolder().length() > 0)
		{
			if (!getParentOfTopWrittenFolder().isDirectory())
			{
				return "The top written path must be a folder";
			}
			
			String topWrittenFolderAbsPath = getParentOfTopWrittenFolder().getAbsolutePath();
			
			//top written folder should be a parent of all source files.
			for (int i = 0; i < sourceFiles_.size(); i++)
			{
				
				if (!sourceFiles_.get(i).getAbsolutePath().startsWith(topWrittenFolderAbsPath))
				{
					return "The virtual drive root must be a parent of all files to backup.";
				}
			}
		}
		
		return null;
	}
}