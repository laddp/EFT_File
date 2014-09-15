/**
 * 
 */
package com.bottinifuel.EFT_File.Records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;

/**
 * @author adminpat
 *
 */
public class BatchControlTotal extends EFT_Record {
	public final BatchHeader BatchStart;
	
	public  final int        ServiceClassCode;
	private       int        EntryAddendaCount;
	private       BigInteger EntryHash;
	private       BigDecimal TotalDebits;
	private       BigDecimal TotalCredits;
	public  final BigInteger CompanyID;
	public  final String     MessageIDCode;
	public  final BigInteger OriginatorDFI;
	public  final BigInteger BatchNumber;
	
	public BatchControlTotal(int recnum, String data, BatchHeader start) throws ParseException, Exception
    {
		super(RecordTypeEnum.BATCH_CONTROL_TOTAL, recnum, data);
		BatchStart = start;
		ServiceClassCode  = Integer.parseInt(data.substring( 2,  5));
		EntryAddendaCount = Integer.parseInt(data.substring( 5, 11));
		EntryHash         = new BigInteger  (data.substring(11, 21));
		
		TotalDebits  = new BigDecimal(data.substring(21, 33)).movePointLeft(2);
		TotalCredits = new BigDecimal(data.substring(33, 45)).movePointLeft(2);
		
		CompanyID    = new BigInteger(data.substring(45, 55));
		MessageIDCode = data.substring(55, 74).trim();
		if (data.substring(74,80).trim().length() != 0)
			throw new ParseException("Reserved field not blank, line #" + recnum, 1);
		
		OriginatorDFI = new BigInteger(data.substring(80, 88));
		BatchNumber    = new BigInteger(data.substring(88));
		
		CheckBatch();
    }
	
	protected void CheckBatch() throws Exception
	{
		if (ServiceClassCode != BatchStart.ServiceClassCode)
			throw new Exception("Batch check error: ServiceClassCode mismatch, line #" + RecordNum);
		int entryCount = BatchStart.Entries.size() + BatchStart.Addenda.size();
		if (EntryAddendaCount != entryCount)
			throw new Exception("Batch check error: EntryAddendaCount mismatch: Got " + entryCount +
					" - Expecting: " + EntryAddendaCount + " - line #" + RecordNum);
		
		if (BatchStart.Checksum().compareTo(EntryHash) != 0)
			throw new Exception("Batch check error: EntryHash mismatch: Got " + BatchStart.Checksum() +
					" - Expecting: " + EntryHash + " - line #" + RecordNum);
		
		if (BatchStart.TotalDebits().compareTo(TotalDebits) != 0)
			throw new Exception("Batch check error: TotalDebits mismatch: Got " + BatchStart.TotalDebits() +
					" - Expecting: " + TotalDebits + " - line #" + RecordNum);
		if (BatchStart.TotalCredits().compareTo(TotalCredits) != 0)
			throw new Exception("Batch check error: TotalCredits mismatch: Got " + BatchStart.TotalCredits() +
					" - Expecting: " + TotalCredits + " - line #" + RecordNum);
		
		if (CompanyID.compareTo(BatchStart.CompanyID) != 0)
			throw new Exception("Batch check error: CompanyID mismatch, line #" + RecordNum);
		
		if (OriginatorDFI.compareTo(BatchStart.OriginatorDFI) != 0)
			throw new Exception("Batch check error: OriginatorDFI mismatch, line #" + RecordNum);
		
		if (BatchNumber.compareTo(BatchStart.BatchNumber) != 0)
			throw new Exception("Batch check error: BatchNumber mismatch, line #" + RecordNum);
	}

	public int getEntryAddendaCount() {
		return EntryAddendaCount;
	}

	/**
	 * @return the entryHash
	 */
	public BigInteger getEntryHash() {
		return EntryHash;
	}

	/**
	 * @return the totalDebits
	 */
	public BigDecimal getTotalDebits() {
		return TotalDebits;
	}

	/**
	 * @return the totalCredits
	 */
	public BigDecimal getTotalCredits() {
		return TotalCredits;
	}

	protected void removeTransaction(EntryDetail trans)
	{
		EntryAddendaCount--;
		EntryHash = EntryHash.subtract(trans.ReceivingDFI.GetNumberNoChecksum());
		if (trans.IsCredit())
			TotalCredits = TotalCredits.subtract(trans.Amount);
		else
			TotalDebits  = TotalDebits .subtract(trans.Amount);
	}

	/* (non-Javadoc)
	 * @see com.bottinifuel.EFT_File.Records.EFT_Record#dump()
	 */
	@Override
	public String dump()
	{
		String rc = "8";
		rc += String.format("%03d%06d%010d", ServiceClassCode, EntryAddendaCount, EntryHash);
		rc += String.format("%012d%012d%-10s", TotalDebits.movePointRight(2).intValue(), TotalCredits.movePointRight(2).intValue(), CompanyID);
		rc += "                         ";
		rc += String.format("%08d%07d", OriginatorDFI, BatchNumber);
		if (rc.equals(Data))
			return Data;
		else
		{
			System.out.println("Orig: " + Data);
			System.out.println(" New: " + rc);
			return rc;
		}
	}
}
