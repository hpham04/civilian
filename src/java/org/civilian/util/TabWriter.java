/*
 * Copyright (C) 2014 Civilian Framework.
 *
 * Licensed under the Civilian License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.civilian-framework.org/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.civilian.util;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;


/**
 * A PrintWriter implementation to write pretty indented files.
 */
public class TabWriter extends PrintWriter
{
	/**
	 * Sets the default characters used by a new TabWriter to indent a line.
	 * By default a indent consists of a single tab character.
	 */ 
	public static void setDefaultTabChars(String chars)
	{
		if (chars == null)
			throw new IllegalArgumentException("chars null");
		defaultTabChars_ = chars.toCharArray();
	}

	
	/**
	 * Returns the default characters used by a new TabWriter to indent a line.
	 */ 
	public static String getDefaultTabChars()
	{
		return new String(defaultTabChars_);
	}

	
	/**
	 * Creates a TabWriter.
	 * @param out a Writer
	 */
	public TabWriter(Writer out)
	{
		this(out, false);
	}


	/**
	 * Creates a TabWriter.
	 * @param out a Writer
	 * @param autoFlush - a boolean; if true, the println() methods will flush
	 *      the output buffer
	 */
	public TabWriter(Writer out, boolean autoFlush)
	{
		super(out, false);
		autoFlush_ = autoFlush;
	}


	/**
	 * Sets the string used for a tab. The default is a tab character.
	 */
	public void setTabChars(String chars)
	{
		int len = chars.length();
		tabChars_ = new char[len];
		chars.getChars(0, len, tabChars_, 0);
	}


	/**
	 * Sets the characters used to separate lines. The default is
	 * system dependent (e.g. "0xD0xA" on Windows).
	 */
	public void setLineSeparator(String s)
	{
		lineSeparator_ = getSeparatorChars(s);
	}


	/**
	 * Increases the indent tab.
	 */
	public void increaseTab()
	{
		tabCount_++;
	}


	/**
	 * Decreases the indent tab.
	 */
	public void decreaseTab()
	{
		if (tabCount_ > 0)
			tabCount_--;
	}


	/**
	 * Returns the current number of indent tabs.
	 */
	public int getTabCount()
	{
		return tabCount_;
	}


	public void setTabCount(int count)
	{
		if (count < 0)
			throw new IllegalArgumentException();
		tabCount_ = count;
	}

	
	protected void writeNewLineTab()
	{
		for (int j=tabCount_; j>0; j--)
			super.write(tabChars_, 0, tabChars_.length);
		newLineStarted_ = false;
	}


	public boolean newLineStarted()
	{
		return newLineStarted_;
	}


	//-------------------------------------------------------
	// In fact the whole printwriter class is duplicated here
	// since this class should be a printwriter but
	// not increase the performance overhead significantly
	// because of the newline mechanism
	//-------------------------------------------------------
	

	/**
	 * Closes the stream.
	 */
	@Override public void close()
	{
		try
		{
			flush();
			synchronized(lock)
			{
				Writer out = this.out;
				this.out   = ClosedWriter.INSTANCE;
				if (out != null)
					out.close();
			}
		}
		catch(IOException e)
		{
			setError(e);
		}
	}


	/**
	 * Flushes the stream and check its error state.  Errors are cumulative;
	 * once the stream encounters an error, this routine will return true on
	 * all successive calls.
	 * @return true if the print stream has encountered an error, either on the
	 * underlying output stream or during a format conversion.
	 */
	@Override public boolean checkError()
	{
		if (out != ClosedWriter.INSTANCE)
			flush();
		return error_ != null;
	}


	/**
	 * Indicates that an error has occurred.
	 */
	protected void setError(IOException e)
	{
		if (error_ == null)
			error_ = e;
	}

	
	/**
	 * Returns the error or null if none has occurred.
	 */
	public IOException getError()
	{
		return error_;
	}
	

	/**
	 * Writes a single character.
	 */
	@Override public void write(int c)
	{
		if (newLineStarted_)
			writeNewLineTab();
		super.write(c);
	}


	/**
	 * Writes a portion of an array of characters.
	 */
	@Override public void write(char buf[], int off, int len)
	{
		if (newLineStarted_)
			writeNewLineTab();
		super.write(buf, off, len);
	}


	/**
	 * Writes a portion of a string.
	 */
	@Override public void write(String s, int off, int length)
	{
		if (newLineStarted_)
			writeNewLineTab();
		super.write(s, off, length);
	}


	/**
	 * Writes a string. This method cannot be inherited from the Writer class
	 * because it must suppress I/O exceptions.
	 */
	@Override public void write(String s)
	{
		write(s, 0, s.length());
	}


	@Override public void print(char c)
	{
		write(c);
	}


	/**
	 * Write a line end, but only if the current line is not empty.
	 */
	public void printlnIfNotEmpty()
	{
		if (!newLineStarted_)
			println();
	}
	
	
	@Override public void println()
	{
		// empty lines are not indented
		if (newLineStarted_)
			newLineStarted_ = false;
		write(lineSeparator_, 0, lineSeparator_.length);
		if (autoFlush_)
			flush();
		newLineStarted_ = true;
	}


	@Override public void println(boolean value)
	{
		print(value);
		println();
	}


	@Override public void println(char value)
	{
		print(value);
		println();
	}


	@Override public void println(int value)
	{
		print(value);
		println();
	}


	@Override public void println(long value)
	{
		print(value);
		println();
	}


	@Override public void println(float value)
	{
		print(value);
		println();
	}


	@Override public void println(double value)
	{
		print(value);
		println();
	}


	@Override public void println(char[] value)
	{
		print(value);
		println();
	}


	@Override public void println(String value)
	{
		print(value);
		println();
	}


	@Override public void println(Object value)
	{
		print(value);
		println();
	}


	private static char[] getSeparatorChars(String s)
	{
		if (s == null)
			throw new IllegalArgumentException();
		char c[] = new char[s.length()];
		s.getChars(0, s.length(), c, 0);
		return c;
	}
	
	
	@Override public String toString()
	{
		return out.toString();
	}


	private boolean autoFlush_;
	private IOException error_;
	private boolean newLineStarted_ = true;
	private int tabCount_ = 0;
	private char tabChars_[] = defaultTabChars_;
	private char lineSeparator_[] = systemLineSeparator_;
	private static char defaultTabChars_[] = { '\t' };
	private static final char systemLineSeparator_[] = getSeparatorChars(System.getProperty("line.separator"));
}
