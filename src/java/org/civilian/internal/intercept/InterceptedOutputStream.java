package org.civilian.internal.intercept;


import java.io.FilterOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import org.civilian.response.ResponseStreamInterceptor;


/**
 * InterceptedOutputStream is a helper class to implement Response.flushBuffer().
 * If the response output is not intercepted, then the response outputstream or writer
 * is the original servlet outputstream or writer (wrapped in a ResponseWriter).
 * In this case we just need to call resetBuffer on the underlying ServletResponse.
 * If the response output is intercepted then the user has obtained the 
 * following chain of OutpuStreams (until the InterceptedOutputStream)
 * or writer (untile the ResponseWriter)
 * <ol>
 * <li>ServletOutputStream
 * <li>InterceptedStream 1, e.g. GzipOutputStream, ... 
 * <li>InterceptedStream n
 * <li>InterceptedOutputStream, returned by Response.getContentStream()
 * <li>OutputStreamWriter
 * <li>ResponseWriter, returned by Response.getContentWriter()
 * </ol>
 * In case of a resetBuffer() call we still forward it to ServletResponse.resetBuffer().
 * But the writer and outputstream chain needs some handling:
 * <ul>
 * <li>Stations in the chain may have buffered data (e.g. OutputStreamWriter
 * 		may keep an encoding buffer, GzipOutputStream has a deflater buffer, ...).
 * <li>InterceptedStream may have written initialization data (e.g. GzipOutputStream
 * 		initialize writes a gzip header, which is lost when the buffer is reset). 
 * </ul>
 * Therefore we do the following:
 * <ol>
 * <li>InterceptedOutputStream.out is temporarily set to a Nil outputstream.
 * <li>Any Writer on top of the InterceptedOutputStream is flushed (and the data goes into Nil)
 * <li>InterceptedOutputStream.out is reconstructed from the interceptor chain.
 * 		{@link ResponseStreamInterceptor#intercept(OutputStream)} may be called
 * 		multiple times.
 * </ol>
 */
public class InterceptedOutputStream extends FilterOutputStream implements InterceptedOutput
{
	public InterceptedOutputStream(OutputStream originalStream, 
		ResponseStreamInterceptor interceptor)
		throws IOException
	{
		super(RespStreamInterceptorChain.intercept(originalStream, interceptor));
		
		originalStream_ = originalStream;
		interceptor_	= interceptor;
	}
	
	
	public void setWriter(Writer writer)
	{
		writer_ = writer;
	}
	
	
	@Override public void reset()
	{
		try
		{
			if (writer_ != null)
			{
				out = new Nil();
				writer_.flush();
			}
			
			out = RespStreamInterceptorChain.intercept(originalStream_, interceptor_);
		}
		catch(IOException e)
		{
			throw new IllegalStateException("could not reset response output", e);
		}
	}
	
	
	/**
	 * An OutputStream that does nothing.
	 */
	private static class Nil extends OutputStream
	{
		@Override public void write(int b)
		{
		}

		
		@Override public void write(byte b[], int off, int len)
		{
		}
	}

	
	private OutputStream originalStream_;
	private ResponseStreamInterceptor interceptor_;
	private Flushable writer_;
}