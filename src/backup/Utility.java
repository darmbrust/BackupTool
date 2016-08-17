package backup;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility methods
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
public class Utility
{
    public static String getVersionNumber()
    {
        return "1.5";
    }

    private static DecimalFormat twoPlaces = new DecimalFormat("0.00");

    public static String formatBytes(long bytes)
    {
        if (bytes > 1073741824)
        {
            return twoPlaces.format(((float) bytes) / ((float) 1024) / ((float) 1024) / ((float) 1024)) + " GB";
        }
        else if (bytes > 1048576)
        {
            return twoPlaces.format(((float) bytes) / ((float) 1024) / ((float) 1024)) + " MB";
        }
        else if (bytes > 1024)
        {
            return twoPlaces.format(((float) bytes) / ((float) 1024)) + " KB";
        }
        else
        {
            return bytes + " bytes";
        }
    }
    
    private static SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a ' on ' EEE MMM d");
    
    public static String getCurrentTime()
    {
        return sdf.format(new Date(System.currentTimeMillis()));
    }
}
