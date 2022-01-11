
Backup Tool
===

This program is a tool for backing up folders files and folders. It has a nice graphical configuration that allows you to choose what folders you want to backup, where you want to back them up to, and then it does the backup for you. It only copies content that has changed since the last backup, so it runs quickly.
Features

- Written in Java + SWT - works on any platform java runs on - including Linux, Mac, and if your in a pinch, Windows.
- SWT gives you a native feel with file choosers that work properly
- Convenient GUI
- Command line only option for running the synchronize task itself
- Only copies files that have changed
- Gives complete summary of what has changed
- Its Free!

**Requirements**

You will need to have [Java](https://adoptopenjdk.net/) version 1.8 or newer installed to use this program. 

**Download**

(you agree to [this](LICENSE) license by downloading this software)


- [BackupTool-Linux-x86.zip](https://github.com/darmbrust/BackupTool/releases/download/1/BackupTool-Linux-x86.zip)
- [BackupTool-Linux-x86_64.zip](https://github.com/darmbrust/BackupTool/releases/download/1/BackupTool-Linux-x86_64.zip)
- [BackupTool-Windows.zip](https://github.com/darmbrust/BackupTool/releases/download/1/BackupTool-Windows.zip)
- [BackupTool-Windows-x64.zip](https://github.com/darmbrust/BackupTool/releases/download/1/BackupTool-Windows-x64.zip)

Unzip this to a place of your choosing, and run the executeable for your platform.

**Command Options**

If you pass in '-rc' as a command line parameter - it will run the pre-configured backup without displaying a GUI.

If you pass in a '-rg' as a command line parameter - it will not display the configuration window - it will just run the pre-configured backup, with a gui window for status information. This is useful if you want to schedule the backup to run every day.

**Bugs**

Gasp! Yes, this software may have bugs.  You can report them here: https://github.com/darmbrust/BackupTool/issues
