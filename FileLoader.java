package gen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileLoader {

    public static int[] readFile(File file) {
        FileInputStream fileStream = null;
        byte[] arr = new byte[(int) file.length()];
        try {
            fileStream = new FileInputStream(file);

            // Instantiate array
    
            /// read All bytes of File stream
            fileStream.read(arr, 0, arr.length);

            int i = 0;
            int[] m_Rom = new int[arr.length];
            for (byte b : arr) {
                m_Rom[i] = b & 0xFF;
                i++;
            }
            
            return m_Rom;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return null;
    }
    
    public static int[] readZip(String filename) {
        try {
            File f = new File(new FileLoader().getClass().getResource("roms\\" + filename).toURI());
            
            ZipFile zipFile = new ZipFile(f);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                int size = (int) entry.getSize();
                InputStream stream = zipFile.getInputStream(entry);
                
                return loadFromStream(stream, size);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static int[] loadFromStream(InputStream is, int size) {
        byte[] bytes = new byte[size];
        BufferedInputStream bis;

        bis = null;
        int[] m_Rom = new int[size];
        try {
            bis = new BufferedInputStream(is);
            int ret = bis.read(bytes);

            int i = 0;
            for (byte b : bytes) {
                m_Rom[i] = b & 0xFF;
                i++;
            }
        } catch (Exception e) {
            System.err.println("Can't open or read ROM");
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                }
                catch (Exception ex) {
                }
            }
        }
        
        return m_Rom;
    }
    
    public static int[] readZipFile(File file) {
        try {
            ZipFile zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().toLowerCase().endsWith(".gb")) {
                    int size = (int) entry.getSize();
                    InputStream stream = zipFile.getInputStream(entry);
                    
                    return loadFromStream(stream, size);
                }
                if (entry.getName().toLowerCase().endsWith(".gbc")) {
                    int size = (int) entry.getSize();
                    InputStream stream = zipFile.getInputStream(entry);
                    
                    return loadFromStream(stream, size);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
}
