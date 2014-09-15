/**
 * 
 */
package com.bottinifuel.EFT_File;

import java.math.BigInteger;


/**
 * @author pladd
 *
 */
public class ABANumber {
	private char[] Data;
	
	public ABANumber(char[] data) throws NumberFormatException {
		if (data.length != 9)
			throw new NumberFormatException("ABA Number must be 9 digits");
		String s = new String(data);
		if (s.matches(".*\\D+.*"))
			throw new NumberFormatException("ABA Number must be numeric");
		Data = data;
		
		if (!IsValidChecksum())
            throw new NumberFormatException("Checksum incorrect");
	}
	
	
	public boolean IsValidPrefix()
	{
		int prefix =
		(Character.getNumericValue(Data[0]) * 10) +
		 Character.getNumericValue(Data[1]);

		if ((prefix > 12 && prefix < 21) ||
		    (prefix > 32 && prefix < 61) ||
		    (prefix > 72))
			return false;
		else
			return true;
	}
	
	public boolean IsValidChecksum() throws NumberFormatException
	{
        int checksum =
            (3 * Character.getNumericValue(Data[0])) +
            (7 * Character.getNumericValue(Data[1])) +
            (1 * Character.getNumericValue(Data[2])) +
            (3 * Character.getNumericValue(Data[3])) +
            (7 * Character.getNumericValue(Data[4])) +
            (1 * Character.getNumericValue(Data[5])) +
            (3 * Character.getNumericValue(Data[6])) +
            (7 * Character.getNumericValue(Data[7])) +
            (1 * Character.getNumericValue(Data[8]));

        if ((checksum % 10) != 0)
        	return false;
        else
        	return true;
	}

    public BigInteger GetNumberNoChecksum() throws NumberFormatException
    {
        try {
            char routingNum[] = new char[8];
            System.arraycopy(Data, 0, routingNum, 0, 8);
            String s = new String(routingNum);
            s = s.trim();
            return new BigInteger(s);
        }
        catch (NumberFormatException e)
        {
            throw e;
        }
    }

    public BigInteger GetNumber() throws NumberFormatException
    {
        try {
            String s = new String(Data);
            s = s.trim();
            return new BigInteger(s);
        }
        catch (NumberFormatException e)
        {
            throw e;
        }
    }
    
    public String toString()
    {
    	return new String(Data);
    }
}
