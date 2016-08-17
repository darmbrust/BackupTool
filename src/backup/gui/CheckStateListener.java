package backup.gui;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;

/**
 * This class handles the logic for checking / unchecking / greying items 
 * in the file tree.
 * 
 * If a parent is checked - children should be grey checked or unchecked, etc.
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
public class CheckStateListener implements ICheckStateListener
{
	private BackupTool backupTool_;
	private CheckboxTreeViewer ctv_;
	public CheckStateListener(BackupTool backupTool, CheckboxTreeViewer ctv)
	{
		backupTool_ = backupTool;
		ctv_  = ctv;
	}
	ArrayList<File> greyItems_ = new ArrayList<File>();
	ArrayList<File> checkedItems_ = new ArrayList<File>();

	public void checkStateChanged(CheckStateChangedEvent event)
	{
		CheckboxTreeViewer ctv = (CheckboxTreeViewer) event.getSource();
		File startFile = (File) event.getElement();
		// If the item is checked . . .
		if (event.getChecked())
		{
			//run through the checked Items - and see if any of them are children of 
			//this item.  If Yes - they don't need to be checked anymore - since the new 
			//parent check will encompass this.
			for (int i = 0; i < checkedItems_.size(); i++)
			{
				File current = checkedItems_.get(i);
				while (true)
				{
					current = current.getParentFile();
					if (current == null)
					{
						break;
					}
					if (current.equals(startFile))
					{
						ctv.setChecked(checkedItems_.get(i), false);
						checkedItems_.remove(i);
						i--;  //array is one smaller now - need to backup.
						break;
					}
				}
			}
			
			//if a parent is checked, make this a grey check.
			//if a parent is gray checked - this is unneeded - cancel the check
			File parent = startFile;
			boolean parentChecked = false;
			boolean parentGrayed = false;
			
			while (true)
			{
				parent = parent.getParentFile();
				if (parent == null)
				{
					break;
				}
				if (ctv.getChecked(parent))
				{
					parentChecked = true;
				}
				if (ctv.getGrayed(parent))
				{
					parentGrayed = true;
				}
				if (parentChecked || parentGrayed)
				{
					break;
				}
			}

			if (parentChecked)
			{
				ctv.setChecked(startFile, false);
				ctv.setGrayChecked(startFile, true);
				greyItems_.add(startFile);
				
				//run through the grey checked Items - and see if any of them are children of 
				//this item.  If Yes - they don't need to be checked anymore - since the new 
				//parent check will encompass this.
				for (int i = 0; i < greyItems_.size(); i++)
				{
					File current = greyItems_.get(i);
					while (true)
					{
						current = current.getParentFile();
						if (current == null)
						{
							break;
						}
						if (current.equals(startFile))
						{
							ctv.setChecked(greyItems_.get(i), false);
							greyItems_.remove(i);
							i--;  //array is one smaller now - need to backup.
							break;
						}
					}
				}
			}
			if (parentGrayed)
			{
				ctv.setChecked(startFile, false);
				ctv.setGrayChecked(startFile, false);
			}
			
			//If I didn't decide to uncheck it - it was left checked.
			if (!parentChecked && !parentGrayed)
			{
				checkedItems_.add(startFile);
			}
		}
		else
		{
			//item was unchecked.
			//need to uncheck any gray items under the check box.  Should be impossible to have checked items 
			//under a checked item.
			
			checkedItems_.remove(startFile);
			greyItems_.remove(startFile);
			ctv.setGrayChecked(startFile, false);
			
			for (int i = 0; i < greyItems_.size(); i++)
			{
				File current = greyItems_.get(i);
				while (true)
				{
					if (current == null)
					{
						break;
					}
					if (current.equals(startFile))
					{
						ctv.setGrayChecked(greyItems_.get(i), false);
						ctv.setChecked(greyItems_.get(i), false);
						checkedItems_.remove(greyItems_.get(i));
						greyItems_.remove(i);
						i--;
						break;
					}
					current = current.getParentFile();
				}
			}
		}
		backupTool_.update(checkedItems_, greyItems_);
	}
	
	protected ArrayList<String> restoreSelections(ArrayList<File> checkedItems, ArrayList<File> greyItems)
	{
		this.checkedItems_ = checkedItems == null ? new ArrayList<File>() : checkedItems;
		this.greyItems_ = greyItems == null ? new ArrayList<File>() : greyItems;
		
		ArrayList<String> warnings = new ArrayList<String>();
		
		for (int i = 0; i < this.checkedItems_.size(); i++)
		{
			File current = checkedItems_.get(i);
			
			if (!current.exists())
			{
				warnings.add(current.getAbsolutePath());
				continue;
			}
			
			//I need to open the tree view down to this item.
			ctv_.expandToLevel(current, 0);
			ctv_.setChecked(current, true);
		
		}
		
		for (int i = 0; i < this.greyItems_.size(); i++)
		{
			File current = greyItems_.get(i);
			//I need to open the tree view down to this item.
			
			if (!current.exists())
			{
				warnings.add(current.getAbsolutePath());
				continue;
			}
			
			ctv_.expandToLevel(current, 0);
			ctv_.setGrayChecked(current, true);
			
		}
		
		
		return warnings;
	}

}
