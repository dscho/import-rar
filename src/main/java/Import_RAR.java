/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Import RAR
 *
 * @author Johannes Schindelin
 */
public class Import_RAR extends ImagePlus implements PlugIn {

	/**
	 * @see ij.plugin.filter.PlugIn#run(String)
	 */
	@Override
	public void run(String arg) {
		final List<ImagePlus> images;
		try {
			if (arg != null && !"".equals(arg))
				images = open(new File(arg), true);
			else {
				OpenDialog od = new OpenDialog("Import RAR...", arg);
				String name = od.getFileName();
				if (name == null)
					return;
				images = open(new File(od.getDirectory(), name), true);
			}
		} catch (Exception e) {
			IJ.handleException(e);
			return;
		}
		if (images.size() > 0)
			setStack(images.remove(0).getStack());
		for (ImagePlus imp : images)
			imp.show();
	}

	/**
	 * Open all images in a .rar file
	 * 
	 * @param image the image (possible multi-dimensional)
	 */
	public List<ImagePlus> open(final File file, boolean showProgress) throws RarException, IOException {
		final List<ImagePlus> result = new ArrayList<ImagePlus>();
		final Archive archive = new Archive(file);
		final List<FileHeader> headers = archive.getFileHeaders();
		Collections.sort(headers, new Comparator<FileHeader>() {
			@Override
			public int compare(FileHeader a, FileHeader b) {
				return a.getFileNameString().compareTo(b.getFileNameString());
			}

			@Override
			public boolean equals(Object o) {
				return o == this;
			}
		});
		int count = 0;
		for (FileHeader header : headers) {
			count++;
			String name = header.getFileNameString();
			if (!name.endsWith(".jpg") && !name.endsWith(".png")) {
				continue;
			}
			name = name.substring(name.lastIndexOf('\\') + 1);
			name = name.substring(name.lastIndexOf('/') + 1);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			archive.extractFile(header, out);
			final InputStream in = new ByteArrayInputStream(out.toByteArray());
			result.add(new ImagePlus(name, ImageIO.read(in)));
			if (showProgress) {
				IJ.showProgress(count, headers.size());
				IJ.showStatus("Imported " + name);
			}
		}
		if (showProgress) {
			IJ.showProgress(0, 0);
			IJ.showStatus("Imported " + file);
		}
		return result;
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ,
	 * and loads all images in a .rar file, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		final String path = args.length > 0 ? args[0] : "/tmp/images.rar";
		// start ImageJ
		new ImageJ();

		// run the plugin
		new Import_RAR().run(path);
	}
}
