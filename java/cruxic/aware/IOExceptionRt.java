package cruxic.aware;

import java.io.IOException;

/**
	Wrap an IOException in a RuntimeException (make it an unchecked exception)
 */
public class IOExceptionRt extends RuntimeException
{
	public IOExceptionRt(IOException cause)
	{
		super(cause);
	}

	public IOExceptionRt(String message)
	{
		super(message);
	}
}
