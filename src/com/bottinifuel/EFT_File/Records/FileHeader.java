/**
 * 
 */
package com.bottinifuel.EFT_File.Records;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.bottinifuel.EFT_File.ABANumber;

/**
 * @author Patrick Ladd
 *
 */
public class FileHeader extends EFT_Record {
	public List<BatchHeader> Batches = new LinkedList<BatchHeader>();
	public FileControl EndFile = null;

	public final int Priority;
	public final ABANumber Destination;
	public final BigInteger Origin;
	public final Date CreationDateTime;
	public final char FileID;
	public final int RecordSize;
	public final int BlockingFactor;
	public final int FormatCode;
	public final String DestinationName;
	public final String OriginName;
	public final String ReferenceCode;
	
	public FileHeader(int recnum, String data) throws NumberFormatException, ParseException, Exception
    {
		super(RecordTypeEnum.FILE_HEADER, recnum, data);
		Priority = Integer.valueOf(data.substring(2, 4));
		if (data.charAt(4) != ' ')
			throw new ParseException("No leading blank on ABA number: FILE_HEADER line #" + recnum, 4);
		Destination = new ABANumber(data.substring(5, 14).toCharArray());
		Origin = new BigInteger(data.substring(14,24));
		
		DateFormat df = new SimpleDateFormat("yyMMddHHmm");
		CreationDateTime = df.parse(data.substring(24,34));
		
		FileID = data.charAt(34);
		
		RecordSize = Integer.valueOf(data.substring(35,38));
		if (RecordSize != 94)
			throw new Exception("Non-standard record size: " + RecordSize + " - expected 94");
		
		BlockingFactor = Integer.valueOf(data.substring(38,40));
		if (BlockingFactor != 10)
			throw new Exception("Non-standard blocking factor: " + BlockingFactor + " - expected 10");
		
		FormatCode = Integer.valueOf("" + data.charAt(40));
		if (FormatCode != 1)
			throw new Exception("Non-standard format code: " + FormatCode + " - expected 1");
		
		DestinationName = data.substring(41, 64).trim();
		OriginName      = data.substring(64, 87).trim();
		ReferenceCode   = data.substring(87).trim();
    }

//	public String dump()
//	{
//		String rc = "1";
//		rc += String.format("%02d %09d%010d", Priority, Destination.GetNumber(), Origin);
//		rc += String.format("%ty%tm%td%tH%tM",
//				CreationDateTime, CreationDateTime, CreationDateTime, CreationDateTime, CreationDateTime);
//		rc += String.format("%c%03d%02d%1d", FileID, RecordSize, BlockingFactor, FormatCode);
//		rc += String.format("%-23s%-23s%-8s", DestinationName, OriginName, ReferenceCode);
//		if (rc.equals(Data))
//			return Data;
//		else
//		{
//			System.out.println(Data);
//			System.out.println(rc);
//			return rc;
//		}
//	}
}
