package backup.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Label Provider for the file tree - handle the content handed out by the 
 * FileTreeContentProvider
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

public class FileTreeLabelProvider implements ILabelProvider
{
	private Display display_;

	private Hashtable<String, Image> images_ = new Hashtable<String, Image>();

	public FileTreeLabelProvider(Display display)
	{
		display_ = display;
	}

	public Image getImage(Object arg0)
	{
		File file = (File) arg0;
		String extension = "";

		try
		{
			if (file.getAbsolutePath().endsWith(":\\"))
			{
				extension = file.getAbsolutePath();
			}
			else if (file.isDirectory())
			{
				extension = "-folder-";
			}
			else
			{

				int pos = (file.getName().lastIndexOf('.'));
				if (pos > 0)
				{
					extension = file.getName().substring(pos, file.getName().length())
							.toLowerCase();
				}
			}

			Image image = images_.get(extension);

			if (image != null)
			{
				return image;
			}
			else
			{
				// SWT doesn't yet have a way to get system images for files.
				// Swing does - use that - and convert.
				Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
				BufferedImage buffImage = new BufferedImage(icon.getIconWidth(), icon
						.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

				// Draw Image into BufferedImage
				Graphics g = buffImage.getGraphics();

				icon.paintIcon(null, g, 0, 0);

				ImageData imageData = convertToSWT(buffImage);

				image = ((imageData != null) ? new Image(display_, imageData) : null);

				images_.put(extension, image);
				return image;
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}

	}

	private static ImageData convertToSWT(BufferedImage bufferedImage)
	{
		if (bufferedImage.getColorModel() instanceof DirectColorModel)
		{
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel
					.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[4];
			for (int y = 0; y < data.height; y++)
			{
				for (int x = 0; x < data.width; x++)
				{
					raster.getPixel(x, y, pixelArray);
					int pixel;
					if (pixelArray[3] < 100)
					{
						// if the transparancy is low - set it to white.
						pixel = palette.getPixel(new RGB(255, 255, 255));
					}
					else
					{
						pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1],
								pixelArray[2]));
					}

					data.setPixel(x, y, pixel);

				}
			}
			return data;
		}
		else if (bufferedImage.getColorModel() instanceof IndexColorModel)
		{
			IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++)
			{
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++)
			{
				for (int x = 0; x < data.width; x++)
				{
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		return null;
	}

	public String getText(Object arg0)
	{
		File file = (File) arg0;
		if (file.getName().length() == 0)
		{
			return file.getAbsolutePath();
		}
		else
		{
			return file.getName();
		}
	}

	public void addListener(ILabelProviderListener arg0)
	{
		// noop

	}

	public void dispose()
	{
		Enumeration<Image> en = images_.elements();
		while (en.hasMoreElements())
		{
			en.nextElement().dispose();
		}

		images_.clear();
	}

	public boolean isLabelProperty(Object arg0, String arg1)
	{
		// I dunno
		return false;
	}

	public void removeListener(ILabelProviderListener arg0)
	{
		//noop

	}

}
