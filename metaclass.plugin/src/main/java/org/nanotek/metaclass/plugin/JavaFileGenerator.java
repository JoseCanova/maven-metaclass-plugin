package org.nanotek.metaclass.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.Manifest;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

public class JavaFileGenerator implements IResultSaver {

  private File output;

public JavaFileGenerator(File output) {
	  this.output = output;
  }
  @Override
  public void saveFolder(String path) {

  }

  @Override
  public void copyFile(String source, String path, String entryName) {

  }

  @Override
  public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
    try {
		FileOutputStream fos =  new FileOutputStream(output);
		fos.write(content.getBytes());
		fos.close();
	} catch (Exception e) {
		e.printStackTrace();
	}
  }

  @Override
  public void createArchive(String path, String archiveName, Manifest manifest) {

  }

  @Override
  public void saveDirEntry(String path, String archiveName, String entryName) {

  }

  @Override
  public void copyEntry(String source, String path, String archiveName, String entry) {

  }

  @Override
  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    try {
		FileOutputStream fos =  new FileOutputStream(output);
		fos.write(content.getBytes());
		fos.close();
	} catch (Exception e) {
		e.printStackTrace();
	}
  }

  @Override
  public void closeArchive(String path, String archiveName) {

  }
}
