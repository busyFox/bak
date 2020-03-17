package com.gotogames.common.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleTools {
	
	/**
	 * Read a string on console input (System.in)
	 * @return
	 * @throws IOException
	 */
	public static String readString() throws IOException {
		String read = null;
		BufferedReader bufferIn = new BufferedReader(new InputStreamReader(System.in));
		read = bufferIn.readLine();
		return read;
	}
	
	/**
	 * Read a string on console input (System.in) and convert it to int
	 * @return
	 * @throws NumberFormatException, IOException
	 */
	public static int readInt() throws NumberFormatException, IOException {
		int read = -1;
		BufferedReader bufferIn = new BufferedReader(new InputStreamReader(System.in));
		read = Integer.parseInt(bufferIn.readLine());
		return read;
	}
	
	/**
	 * Read a string on console input (System.in) and convert it to long
	 * @return
	 * @throws NumberFormatException, IOException
	 */
	public static long readLong() throws NumberFormatException, IOException {
		long read = -1;
		BufferedReader bufferIn = new BufferedReader(new InputStreamReader(System.in));
		read = Long.parseLong(bufferIn.readLine());
		return read;
	}

    /**
     * Read a string on console input (System.in) and convert it to long
     * @return
     * @throws NumberFormatException, IOException
     */
    public static double readDouble() throws NumberFormatException, IOException {
        double read = 0;
        BufferedReader bufferIn = new BufferedReader(new InputStreamReader(System.in));
        read = Double.parseDouble(bufferIn.readLine());
        return read;
    }
}
