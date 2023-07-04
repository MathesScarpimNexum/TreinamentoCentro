package com.lecom.workflow.cadastros.common.util;


import java.io.*;
import java.util.*;
import java.util.zip.*;

public class UtilZip {

    /**
     * Compress a byte array using the java.util.zip.Deflater object
     * with BEST_COMPRESSION
     * and return a byte array containing the compressed bytes.
     *
     * @param input The byte array to compress.
     * @return byte[] The compressed byte array.
     * @throws Exception If an error occurs.
     */
    public static byte[] compressByteArray(byte[] input) throws Exception {

        //
        // Create the compressor with highest level of compression
        //
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);

        //
        // Give the compressor the data to compress
        //
        compressor.setInput(input);
        compressor.finish();

        //
        // Create an expandable byte array to hold the compressed data.
        // You cannot use an array that's the same size as the orginal because
        // there is no guarantee that the compressed data will be smaller than
        // the uncompressed data.
        //
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        //
        // Compress the data
        //
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }

        try {
            bos.close();
        } catch (IOException e) {
        }

        //
        // Get the compressed data
        //
        byte[] compressedData = bos.toByteArray();

        return compressedData;
    }

    /**
     * Uncompress a compressed byte array using the java.util.zip.Inflater
     * object and return a byte array containing the uncompressed bytes.
     *
     * @param compressedData The byte array to uncompress.
     * @return byte[] The uncompressed byte array.
     * @throws Exception If an error occurs.
     */
    public static byte[] uncompressByteArray(byte[] compressedData) throws Exception {
    	
        //
        // Create the decompressor and give it the data to compress
        //
        Inflater decompressor = new Inflater();
        decompressor.setInput(compressedData);

        //
        // Create an expandable byte array to hold the decompressed data
        //
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);

        //
        // Decompress the data
        //
        byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            try {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            } catch (DataFormatException e) {
            }
        }
        
        try {
            bos.close();
        } catch (IOException e) {
        }

        //
        // Get the decompressed data
        //
        byte[] decompressedData = bos.toByteArray();

        return decompressedData;
    }

    
    public static void packZip(File output, List<File> sources) throws IOException {
        
    	ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output));
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);

        for (File source : sources) {
        	
            if (source.isDirectory()) {
                zipDir(zipOut, "", source);
            } else {
                zipFile(zipOut, "", source);
            }
        }
        
        zipOut.flush();
        zipOut.close();
    }

    private static String buildPath(String path, String file) {
        
    	if (path == null || path.isEmpty()) {
            return file;
        } else {
            return path + "/" + file;
        }
    }

    private static void zipDir(ZipOutputStream zos, String path, File dir) throws IOException {
        
    	if (!dir.canRead()) {
            return;
        }

        File[] files = dir.listFiles();
        path = buildPath(path, dir.getName());

        for (File source : files) {
            
        	if (source.isDirectory()) {
                zipDir(zos, path, source);
            } else {
                zipFile(zos, path, source);
            }
        }

    }

    private static void zipFile(ZipOutputStream zos, String path, File file) throws IOException {
        
    	if (!file.canRead()) {
            return;
        }

        zos.putNextEntry(new ZipEntry(buildPath(path, file.getName())));

        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[4092];
        int byteCount = 0;
        while ((byteCount = fis.read(buffer)) != -1) {
            zos.write(buffer, 0, byteCount);
        }

        fis.close();
        zos.closeEntry();
    }
    
    public static void unzipFile(File arquivoZip,String pastaDestino) throws Exception{
    	
    	FileInputStream fin = new FileInputStream(arquivoZip);
        ZipInputStream zin = new ZipInputStream(fin);
        ZipEntry ze = null;
        
        while ((ze = zin.getNextEntry()) != null) {
        	
          File arquivoNovo = new File(pastaDestino+ze.getName());
          
          if (arquivoNovo.exists()){
        	  arquivoNovo.delete();
          }
          
          arquivoNovo.createNewFile();
          FileOutputStream fout = new FileOutputStream(arquivoNovo);
          
          for (int c = zin.read(); c != -1; c = zin.read()) {
            fout.write(c);
          }
          
          zin.closeEntry();
          fout.close();
        }
        
        zin.close();
    }
    
}