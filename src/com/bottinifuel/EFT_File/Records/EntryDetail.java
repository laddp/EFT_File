/**
 * 
 */
package com.bottinifuel.EFT_File.Records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;

import com.bottinifuel.EFT_File.ABANumber;

/**
 * @author adminpat
 *
 */
public class EntryDetail extends EFT_Record {
	public EntryDetailAddenda Addenda = null;
	
	public final int         TransactionCode;
	public final ABANumber   ReceivingDFI;
	public final String      AccountNum;
	public final BigDecimal  Amount;
	public final String      IndividualID;
	public final String      IndividualName;
	public final String      DiscretionaryData;
	public final boolean     AddendaRecordIndicator;
	public final BigInteger  TraceNumber;
	public final BatchHeader Batch;
	
	public EntryDetail(int recnum, String data, BatchHeader batch, boolean allowInvalidPrefix) throws ParseException
    {
		super(RecordTypeEnum.ENTRY_DETAIL, recnum, data);
		Batch = batch;
		
		TransactionCode = Integer.parseInt(data.substring(2, 4));
		ReceivingDFI    = new ABANumber(data.substring(4, 13).toCharArray());
		AccountNum      = data.substring(13, 30).trim();
		Amount          = new BigDecimal(data.substring(30, 40)).movePointLeft(2);
		IndividualID    = data.substring(40, 55).trim();
		IndividualName  = data.substring(55, 77).trim();
		DiscretionaryData = data.substring(77, 79).trim();
		if (data.charAt(79) == '0')
			AddendaRecordIndicator = false;
		else if (data.charAt(79) == '1')
			AddendaRecordIndicator = true;
		else
			throw new ParseException("Addenda indicator not 0 or 1. Got: " + data.charAt(79) + " line #" + recnum, 79);
		TraceNumber = new BigInteger(data.substring(80));
		
		if (!allowInvalidPrefix && !ReceivingDFI.IsValidPrefix())
			throw new ParseException("Invalid Deposit Prefix: " + ReceivingDFI.toString() + " Account #" + AccountNum + " Name: " + IndividualName, 0);
    }
	
	public boolean IsCredit()
	{
		if (TransactionCode == 22 ||
		 	TransactionCode == 32)
			return true;
		else
			return false;
	}
	
	public boolean IsDebit()
	{
		if (TransactionCode == 27 ||
			TransactionCode == 37)
			return true;
		else
			return false;
	}
}
