package backup.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * This class reads and writes the BackupConfigs to and from XML.
 * It stores the backup in the users home directory.
 * C:\Documents and Settings\{user}\My Documents\Backup Configuration\BackupConfig.xml
 * on windows.
 * /home/{user}/Backup Configuration/BackupConfig.xml on unix.
 * 
 * Unless overridden by a system variable "-DConfigFile=C:\foo"
 * 
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
public class BackupConfigStore
{

	public static File locateConfig()
	{
		String config = System.getProperty("ConfigFile");
		if (config != null && config.length() > 0)
		{
			return new File(config);
		}
		
		String home = System.getProperty("user.home");

		File temp = new File(home);
		if (!temp.exists())
		{
			return null;
		}
		//TODO this isn't right on Windows 7
		File myDocs = new File(temp, "My Documents");
		File saveFolder;
		if (myDocs.exists())
		{
			saveFolder = new File(myDocs, "Backup Configuration");
		}
		else
		{
			saveFolder = new File(temp, ".Backup Configuration");
		}

		saveFolder.mkdir();

		if (!saveFolder.exists())
		{
			return null;
		}

		File configFile = new File(saveFolder, "BackupConfig.xml");

		return configFile;
	}
	
	
	public static void writeConfigs(File fileLocation, ArrayList<BackupConfig> configs)
			throws IOException
	{
		fileLocation.createNewFile();
		Document document = new Document(new Element("BackupConfigs"));
		Element root = document.getRootElement();

		for (int i = 0; i < configs.size(); i++)
		{
			Element temp = new Element("BackupConfig");
			temp.setAttribute("targetFile", (configs.get(i).getTargetFile() == null ? "" : configs
					.get(i).getTargetFile().getAbsolutePath()));
			temp.setAttribute("timeStampLeniancy", configs.get(i).getTimeStampLeniancy() + "");
			temp.setAttribute("removeObsolete", configs.get(i).getRemoveObsoleteFiles() + "");
			temp.setAttribute("copyThreads", configs.get(i).getCopyThreads() + "");
			temp.setAttribute("topWrittenFolder", (configs.get(i).getParentOfTopWrittenFolder() == null ? "" : configs
					.get(i).getParentOfTopWrittenFolder().getAbsolutePath()));

			for (int j = 0; j < configs.get(i).getSourceFiles().size(); j++)
			{
				Element sub1 = new Element("SourceFile");
				sub1.setAttribute("fileName", configs.get(i).getSourceFiles().get(j)
						.getAbsolutePath());
				temp.addContent(sub1);
			}

			for (int j = 0; j < configs.get(i).getFilesToSkip().size(); j++)
			{
				Element sub2 = new Element("FileToSkip");
				sub2.setAttribute("fileName", configs.get(i).getFilesToSkip().get(j)
						.getAbsolutePath());
				temp.addContent(sub2);
			}
            
            for (int j = 0; j < configs.get(i).getGlobalFilesToSkip().size(); j++)
            {
                Element sub2 = new Element("GlobalFileToSkip");
                sub2.setAttribute("fileName", configs.get(i).getGlobalFilesToSkip().get(j));
                temp.addContent(sub2);
            }

			root.addContent(temp);
		}

		writeFile(fileLocation, document);

	}

	@SuppressWarnings("unchecked")
	public static ArrayList<BackupConfig> readConfigs(File fileLocation) throws JDOMException,
			IOException
	{
		Document doc = readFile(fileLocation);
		Element root = doc.getRootElement();
		List<Element> list = root.getChildren("BackupConfig");

		ArrayList<BackupConfig> bc = new ArrayList<BackupConfig>(list.size());

		for (int i = 0; i < list.size(); i++)
		{
			Element element = (Element) list.get(i);
			BackupConfig temp = new BackupConfig();

			temp.setRemoveObsoleteFiles(new Boolean(element.getAttributeValue("removeObsolete"))
					.booleanValue());
			temp.setTargetFile((element.getAttributeValue("targetFile").length() == 0 ? null
					: new File(element.getAttributeValue("targetFile"))));

			temp.setTimeStampLeniancy(Integer.parseInt(element
					.getAttributeValue("timeStampLeniancy")));
			
			temp.setParentOfTopWrittenFolder(((element.getAttributeValue("topWrittenFolder") == null || element.getAttributeValue("topWrittenFolder").length() == 0) ? null
					: new File(element.getAttributeValue("topWrittenFolder"))));

			temp.setCopyThreads(Integer.parseInt(element.getAttributeValue("copyThreads")));

			List<Element> sub = element.getChildren("SourceFile");
			ArrayList<File> sourceFiles = new ArrayList<File>(sub.size());

			for (int j = 0; j < sub.size(); j++)
			{
				sourceFiles.add(new File(((Element) sub.get(j)).getAttributeValue("fileName")));
			}

			temp.setSourceFiles(sourceFiles);

			sub = element.getChildren("FileToSkip");
			ArrayList<File> filesToSkip = new ArrayList<File>(sub.size());

			for (int j = 0; j < sub.size(); j++)
			{
				filesToSkip.add(new File(((Element) sub.get(j)).getAttributeValue("fileName")));
			}

			temp.setFilesToSkip(filesToSkip);
            
            sub = element.getChildren("GlobalFileToSkip");
            ArrayList<String> globalFilesToSkip = new ArrayList<String>(sub.size());

            for (int j = 0; j < sub.size(); j++)
            {
                globalFilesToSkip.add(((Element) sub.get(j)).getAttributeValue("fileName"));
            }

            temp.setGlobalFilesToSkip(globalFilesToSkip);

			bc.add(temp);

		}

		return bc;
	}

	private static Document readFile(File file) throws JDOMException, IOException
	{
		SAXBuilder saxBuilder = new SAXBuilder();
		return saxBuilder.build(file);
	}

	private static void writeFile(File file, Document document) throws IOException
	{
		XMLOutputter xmlFormatter = new XMLOutputter(Format.getPrettyFormat());

		FileWriter writer = new FileWriter(file);
		writer.write(xmlFormatter.outputString(document));
		writer.close();

	}
}