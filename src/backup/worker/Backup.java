package backup.worker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import backup.config.BackupConfig;

/**
 * This is the class that actually does all the work of copying folders to the backup location. Handles
 * checking to see if they have changed, removing obsoletes, etc. Multi-threaded.
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
public class Backup
{
    private volatile boolean   finishedScanning     = false;

    private boolean            removeObsoleteFiles_;

    private int                timeStampLeniancy_   = 50;

    private ArrayList<FilePair>filesToCopy_         = new ArrayList<FilePair>();

    private Vector<Failure>    failures             = new Vector<Failure>();

    private ArrayList<String>  copiedFiles          = new ArrayList<String>();

    private long               bytesCopied          = 0;

    private AtomicInteger      obsoleteFileRemoveCount_ = new AtomicInteger(0);
    
    private File			   virtualPathRoot_		= null;

    private HashSet<File>      filesToSkip_         = new HashSet<File>();

    private HashSet<String>    globalFilesToSkip_   = new HashSet<String>();

    private HashSet<String>    filesThatShouldExist = new HashSet<String>();

    private int                fileScanCount_       = 0;

    private String             currentFileScanFolder_ = "";
    
    private CopyThread[]	   copyThreads_ = null;

    public Backup(BackupConfig bc)
    {
        start(bc);
    }

    private void start(BackupConfig bc)
    {
        removeObsoleteFiles_ = bc.getRemoveObsoleteFiles();
        timeStampLeniancy_ = bc.getTimeStampLeniancy();
        virtualPathRoot_ = bc.getParentOfTopWrittenFolder();

        if (bc.getFilesToSkip() != null)
        {
            for (int i = 0; i < bc.getFilesToSkip().size(); i++)
            {
                filesToSkip_.add(bc.getFilesToSkip().get(i));
            }
        }

        if (bc.getGlobalFilesToSkip() != null)
        {
            for (int i = 0; i < bc.getGlobalFilesToSkip().size(); i++)
            {
                globalFilesToSkip_.add(bc.getGlobalFilesToSkip().get(i));
            }
        }

        copyThreads_ = new CopyThread[bc.getCopyThreads()];
        for (int i = 0; i < bc.getCopyThreads(); i++)
        {
        	copyThreads_[i] = new CopyThread();
            // start up the copy threads
            Thread thread = new Thread(copyThreads_[i]);
            thread.setDaemon(true);
            thread.start();
        }

        Scanner s = new Scanner(bc.getSourceFiles(), bc.getTargetFile());
        Thread scanThread = new Thread(s);
        scanThread.setDaemon(true);
        scanThread.start();

    }

    public String getLastCopiedFile()
    {
        if (copiedFiles.size() > 0)
        {
            return copiedFiles.get(copiedFiles.size() - 1);
        }
        else
        {
            return null;
        }

    }

    public boolean finishedCopying()
    {
        if (finishedScanning && filesToCopy_.size() == 0)
        {
        	for (int i = 0; i < copyThreads_.length; i++)
			{
        		if (!copyThreads_[i].isFinished())
        		{
        			//this thread is still busy.
        			return false;
        		}
			}
        	//If we get here, all of the copyThreads are finished.
        	return true;
        }
        else
        {
        	return false;
        }
    }
    

    public Failure[] getFailures()
    {
        return failures.toArray(new Failure[failures.size()]);
    }

    /*
     * 
     * @param sourceFolder - the folder to duplicate. @param targetFolder - where the duplicate folder should
     * go. This should be a path that ends in the same name as the source folder.
     */
    private void syncFiles(File sourceLocation, File targetLocation)
    {
        if (sourceLocation == null)
        {
            return;
        }
        if (filesToSkip_.contains(sourceLocation) || globalFilesToSkip_.contains(sourceLocation.getName()))
        {
            return;
        }
        
        //see if its extension matches a skip extension
        int pos = sourceLocation.getName().lastIndexOf('.');
        if (pos > 0)
        {
            String ext = "*" + sourceLocation.getName().substring(pos);
            if (globalFilesToSkip_.contains(ext))
            {
                return;
            }
        }
        
        if (removeObsoleteFiles_)
        {
            filesThatShouldExist.add(targetLocation.getAbsolutePath());
        }
        if (sourceLocation.isDirectory())
        {
            if (targetLocation.exists() && targetLocation.isFile())
            {
                // need to delete the file - replace it with a directory.
                boolean success = targetLocation.delete();
                if (!success)
                {
                    failures.add(new Failure("Could not delete a file that needed to be replaced with a directory",
                            targetLocation));
                    return;
                }
            }
            if (!targetLocation.exists())
            {
                if (!targetLocation.mkdir())
                {
                    failures.add(new Failure("Could not create target directory", targetLocation));
                    return;
                }
            }

            currentFileScanFolder_ = sourceLocation.getAbsolutePath();

            // now we know that source and target are both directories
            File[] sources = sourceLocation.listFiles();

            File[] targets = null;
            if (removeObsoleteFiles_)
            {
                // see what is there now.
                targets = targetLocation.listFiles();
            }

            
            if (sources == null)
            {
            	//This should be impossible, but the JVM doesn't handle junctions (or hard links) properly in windows.
            	//In all likelyhood, this is a junction.
            	failures.add(new Failure("Could not copy, is this a link?", sourceLocation));
            }
            else
            {
            	// go through the sources, see if they need to be copied.
	            for (int i = 0; i < sources.length; i++)
	            {
	                String sourceName = sources[i].getName();
	
	                File target = new File(targetLocation, sourceName);
	                syncFiles(sources[i], target);
	            }
            }

            if (removeObsoleteFiles_)
            {
                // now that the files that are supposed to be there have been copied (and recorded)
                // clean up the ones that shouldn't
                for (int i = 0; i < targets.length; i++)
                {
                    if (!filesThatShouldExist.contains(targets[i].getAbsolutePath()))
                    {
                        recursiveDelete(targets[i]);
                    }
                }
            }
        }
        else if (sourceLocation.exists())
        {
            // source file is a file
            fileScanCount_++;
            
            if (targetLocation.exists()
                    && targetLocation.isFile()
                    && sourceLocation.length() == targetLocation.length()
                    && areTimeStampsEqual(sourceLocation.lastModified(), targetLocation.lastModified()))
            {
                // if all of those checks are true, no need to copy the
                // file.
                return;
            }
            else
            {
                if (targetLocation.isDirectory())
                {
                    // need to remove the directory.
                    boolean success = recursiveDelete(targetLocation);
                    if (!success)
                    {
                        failures.add(new Failure("Could not remove a directory that needed to be replaced with a file",
                                targetLocation));
                        return;
                    }
                }
                // file needs to be copied.
                synchronized (filesToCopy_)
				{
                	filesToCopy_.add(new FilePair(sourceLocation, targetLocation));
                	filesToCopy_.notify();
				}
            }
        }
        else
        {
            // source file doesn't exist
            failures.add(new Failure("Source file does not exist", sourceLocation));
        }

    }

    private boolean areTimeStampsEqual(long timeStampOne, long timeStampTwo)
    {
        long temp = timeStampOne - timeStampTwo;
        if (temp < 0)
        {
            temp = temp * -1;
        }
        // ok - now, the difference is positive. If the difference it more than
        // the leniancy figure
        // return false. Else, return true.
        if (temp > timeStampLeniancy_)
        {
            return false;
        }
        else
        {
            return true;
        }

    }

    private boolean recursiveDelete(File file)
    {
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                boolean success = recursiveDelete(files[i]);
                if (!success)
                {
                    return false;
                }
            }
        }
        obsoleteFileRemoveCount_.incrementAndGet();
        return file.delete();
    }

    public class Failure
    {
        private String reason;
        private File file;
        private Throwable cause;

        public Failure(String reason, File file)
        {
            this.reason = reason;
            this.file = file;
        }
        
        public Failure(Throwable cause)
        {
            this.cause = cause;
        }
        
        public String toString()
        {
        	return "Failure: " + (reason != null ? reason + " " :" ") + (file != null ? file.getAbsolutePath() + " " : " ") + 
        	    (cause != null ? cause : "");
        }
    }

    private class FilePair
    {
        File sourceFile, targetFile;

        public FilePair(File sourceFile, File targetFile)
        {
            this.sourceFile = sourceFile;
            this.targetFile = targetFile;
        }
    }

    private void copyFile(File sourceFile, File targetFile)
    {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try
        {
            bis = new BufferedInputStream(new FileInputStream(sourceFile));
        }
        catch (FileNotFoundException e)
        {
            failures.add(new Failure("source file not found or is unreadable", sourceFile));
            return;
        }
        try
        {
            bos = new BufferedOutputStream(new FileOutputStream(targetFile));
        }
        catch (FileNotFoundException e)
        {
            failures.add(new Failure("target file could not be found", targetFile));
            return;
        }
        byte[] buffer = new byte[2048];
        while (true)
        {
            int bytes_read = 0;
            try
            {
                bytes_read = bis.read(buffer);
            }
            catch (IOException e)
            {
                failures.add(new Failure("problem reading source file", sourceFile));
                return;
            }
            if (bytes_read == -1)
            {
                break;
            }
            try
            {
                bos.write(buffer, 0, bytes_read);
            }
            catch (IOException e)
            {
                failures.add(new Failure("problem writing to target file", targetFile));
                return;
            }
        }
        try
        {
            bis.close();
        }
        catch (IOException e)
        {
            ;
        }
        try
        {
            bos.close();
        }
        catch (IOException e)
        {
            ;
        }
        targetFile.setLastModified(sourceFile.lastModified());
        logFileCopy(sourceFile);

    }

    private synchronized void logFileCopy(File file)
    {
        bytesCopied += file.length();
        copiedFiles.add(file.getAbsolutePath());
    }

    private class CopyThread implements Runnable
    {
    	private volatile boolean finished = false;
    	private FilePair currentFilePair = null;
    	
    	public boolean isFinished()
    	{
    		return finished;
    	}
    	
    	public String getCurrentlyCopyingFile()
    	{
    		FilePair temp = currentFilePair;
    		if (temp != null)
    		{
    			return temp.sourceFile.getAbsolutePath();
    		}
    		else
    		{
    			return null;
    		}
    	}
    	
        public void run()
        {
            try
			{
				while (true)
				{
					currentFilePair = null;
					try
					{
						synchronized (filesToCopy_)
						{
						    if (filesToCopy_.size() == 0)
						    {
						    	if (finishedScanning)
						    	{
						    		break;
						    	}
						        try
						        {
						            filesToCopy_.wait();  //wait for notification of more work to do
						        }
						        catch (InterruptedException e)
						        {
						            // do nothing
						        }
						    }
						    else
						    {
						    	currentFilePair = filesToCopy_.remove(0);
						    }
						}
						if (currentFilePair != null)
						{
							copyFile(currentFilePair.sourceFile, currentFilePair.targetFile);
						}
					}
					catch (Exception e)
					{
						failures.add(new Failure("Unexpected error during file copy", (currentFilePair == null ? new File("-unknown-") : currentFilePair.sourceFile)));
					}
				}
			}
			catch (Exception e)
			{
				//This is unexpected
				e.printStackTrace();
			}
			finally
			{
				finished = true;
			}
        }
    }

    private class Scanner implements Runnable
    {
        private ArrayList<File> sourceFiles_;

        private File            targetFile_;

        public Scanner(ArrayList<File> sourceFiles, File targetFile)
        {
            sourceFiles_ = sourceFiles;
            targetFile_ = targetFile;
        }

        public boolean scanningFinished()
        {
            return finishedScanning;
        }

        public void run()
        {
        	try
        	{
	            ArrayList<File> targetLocations = new ArrayList<File>(sourceFiles_.size());
	
	            if (removeObsoleteFiles_)
	            {
	                filesThatShouldExist.add(targetFile_.getAbsolutePath());
	            }
	
	            for (int i = 0; i < sourceFiles_.size(); i++)
	            {
	                File source = sourceFiles_.get(i);
	
	                // I want to mirror the folder structure on the target to where
	                // the source came from.
	                // so I need to create all of the necessary folders on the target
	                // to get to this current source
	                // location.
	                ArrayList<File> path = new ArrayList<File>();
	                File parentFile = source;
	                while (parentFile != null)
	                {
	                    path.add(parentFile);
	                    if (virtualPathRoot_  != null && parentFile.equals(virtualPathRoot_))
	                    {
	                    	//we jump out of the upward traverse early if they provided a virtual path root, and it matches.
	                    	break;
	                    }
	                    parentFile = parentFile.getParentFile();
	                }
	                // got to the root - so we have probably added a C:\ or a '\' or
	                // an equivalent - don't want that.  Remove the root (including the virtual root, if that was used)
	                path.remove(path.size() - 1);
	
	                // now run through this in reverse, creating the appropriate
	                // folders on the target.
	
	                File target = targetFile_;
	                for (int j = path.size() - 1; j >= 0; j--)
	                {
	                    target = new File(target, path.get(j).getName());
	                    if (removeObsoleteFiles_)
	                    {
	                        filesThatShouldExist.add(target.getAbsolutePath());
	                    }
	                    if (!target.exists())
	                    {
	                        target.mkdir();
	                    }
	                }
	                // store this final target for use below
	                targetLocations.add(target);
	            }
	
	            // ok - now - I should have the full path duplicated - and all of the root folders created and
	            // noted.
	            // The targetLocations list is the set of matched target locations for each of the source folders.
	            // run through them (this isn't done above, because I need to know all of them first to the
	            // obsolete file
	            // remover doesn't get confused.
	            for (int i = 0; i < sourceFiles_.size(); i++)
	            {
	                syncFiles(sourceFiles_.get(i), targetLocations.get(i));
	            }
        	}
        	catch (Exception e) 
        	{
        		failures.add(new Failure(e));
        	}
        	finally
        	{
        		finishedScanning = true;
	            synchronized (filesToCopy_)
				{
	            	filesToCopy_.notifyAll();
				}
        	}
        }
    }

    public int getFileCopyCount()
    {
        return copiedFiles.size();
    }

    public int getFileFailureCount()
    {
        return failures.size();
    }

    public String[] getCopiedFiles()
    {
        return copiedFiles.toArray(new String[copiedFiles.size()]);
    }

    public int getObsoleteFileRemovedCount()
    {
        return obsoleteFileRemoveCount_.get();
    }

    public int getFilesRemainingCount()
    {
        return filesToCopy_.size();
    }

    public boolean getFinishedScanning()
    {
        return finishedScanning;
    }

    public int getFileScanCount()
    {
        return fileScanCount_;
    }

    public long getBytesCopied()
    {
        return bytesCopied;
    }

    public String getCurrentScanningFolder()
    {
        return currentFileScanFolder_;
    }
    
    public ArrayList<String> getFilesBeingCopied()
    {
    	ArrayList<String> result = new ArrayList<String>(copyThreads_.length);
    	for (int i = 0; i < copyThreads_.length; i++)
		{
    		String temp = copyThreads_[i].getCurrentlyCopyingFile();
    		if (temp != null)
    		{
    			result.add(temp);
    		}
		}
    	return result;
    }
}
