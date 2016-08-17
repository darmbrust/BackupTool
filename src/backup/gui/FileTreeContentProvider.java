package backup.gui;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content Provider for the file tree - returns the directories.
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
public class FileTreeContentProvider implements ITreeContentProvider
{

	public Object[] getChildren(Object arg0)
	{
		File temp = (File) arg0;
		File[] res = temp.listFiles();
		if (res == null)
		{
			res = new File[] {};
		}
		Arrays.sort(res, new FileComparator());
		return res;
	}

	public Object getParent(Object arg0)
	{
		File file = (File) arg0;
		return file.getParentFile();
	}

	public boolean hasChildren(Object arg0)
	{
		File temp = (File) arg0;
		if (temp.getAbsolutePath().equalsIgnoreCase("A:\\"))
		{
			// don't want drive A errors cause by Sun's 8 year old bug
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4089199
			return false;
		}
		if (temp != null && temp.exists() && temp.isDirectory())

		{
			String[] sub = temp.list();
			if (sub != null && sub.length > 0)
			{
				return true;
			}
		}

		return false;

	}

	public Object[] getElements(Object arg0)
	{
		return File.listRoots();
	}

	public void dispose()
	{
		// nothing

	}

	public void inputChanged(Viewer arg0, Object arg1, Object arg2)
	{
		// don't care

	}

	public class FileComparator implements Comparator<File>
	{

		public int compare(File o1, File o2)
		{
			if (o1.isDirectory() && o2.isFile())
			{
				return -1;
			}
			else if (o2.isDirectory() && o1.isFile())
			{
				return 1;
			}
			else
			{
				return o1.getName().compareTo(o2.getName());
			}
		}

	}

}
