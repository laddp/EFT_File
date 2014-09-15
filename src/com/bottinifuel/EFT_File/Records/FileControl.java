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
public class FileControl extends EFT_Record {
	public final FileHeader FileStart;
	
	public final int        BatchCount;
	private      int        BlockCount;
	private      int        EntryAddendaCount;
	private      BigInteger EntryHash;
	private      BigDecimal TotalDebits;
	private      BigDecimal TotalCredits;

	
	public FileControl(int recnum, String data, FileHeader start) throws ParseException, Exception
    {
		super(RecordTypeEnum.FILE_CONTROL, recnum, data);
		FileStart = start;
		BatchCount        = Integer.parseInt(data.substring( 2,  8));
		BlockCount        = Integer.parseInt(data.substring( 8, 14));
		EntryAddendaCount = Integer.parseInt(data.substring(14, 22));
		EntryHash         = new BigInteger  (data.substring(22, 32));
		
		TotalDebits  = new BigDecimal(data.substring(32, 44)).movePointLeft(2);
		TotalCredits = new BigDecimal(data.substring(44, 56)).movePointLeft(2);

		if (data.substring(56).trim().length() != 0)
			throw new ParseException("Non-blank reserved field, line #" + recnum, 56);
		
		CheckFile();
    }
	
	private void CheckFile() throws Exception
	{
		if (BatchCount != FileStart.Batches.size())
			throw new Exception("File closing error: BatchCount mismatch: Got " + FileStart.Batches.size() +
					" - Expecting: " + BatchCount + " - line #" + RecordNum);
		int blocks;
		if (RecordNum % 10 == 0)
			blocks = RecordNum / 10;
		else
			blocks = (RecordNum / 10) + 1;
		if (blocks != BlockCount)
			throw new Exception("File closing error: BlockCount mismatch: Got " + blocks +
					" - Expecting: " + BlockCount + " - line #" + RecordNum);
		
		int entryCount = 0;
		BigInteger entryHash = new BigInteger("0");
		BigDecimal debits  = new BigDecimal(0);
		BigDecimal credits = new BigDecimal(0);
		for (BatchHeader batch : FileStart.Batches)
		{
			entryCount += batch.BatchEnd.getEntryAddendaCount();
			entryHash = entryHash.add(batch.BatchEnd.getEntryHash());
			debits  = debits. add(batch.BatchEnd.getTotalDebits());
			credits = credits.add(batch.BatchEnd.getTotalCredits());
		}
		
		if (entryCount != EntryAddendaCount)
			throw new Exception("File closing error: EntryAddendaCount mismatch: Got " + entryCount +
					" - Expecting: " + EntryAddendaCount + " - line #" + RecordNum);
		if (entryHash.compareTo(EntryHash) != 0)
			throw new Exception("File closing error: EntryHash mismatch: Got " + entryHash +
					" - Expecting: " + EntryHash + " - line #" + RecordNum);
		if (debits.compareTo(TotalDebits) != 0)
			throw new Exception("File closing error: TotalDebits mismatch: Got " + debits +
					" - Expecting: " + TotalDebits + " - line #" + RecordNum);
		if (credits.compareTo(TotalCredits) != 0)
			throw new Exception("File closing error: TotalCredits mismatch: Got " + credits +
					" - Expecting: " + TotalCredits + " - line #" + RecordNum);
	}

	public int getBlockCount() {
		return BlockCount;
	}

	/**
	 * @param blockCount the blockCount to set
	 */
	public void setBlockCount(int blockCount) {
		BlockCount = blockCount;
	}

	/**
	 * @return the entryAddendaCount
	 */
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

	public void RemoveTransaction(EntryDetail trans) throws Exception
	{
		// TODO: Can't handle reducing block count
		EntryAddendaCount--;
		EntryHash = EntryHash.subtract(trans.ReceivingDFI.GetNumberNoChecksum());
		if (trans.IsCredit())
			TotalCredits = TotalCredits.subtract(trans.Amount);
		else
			TotalDebits  = TotalDebits .subtract(trans.Amount);
		
		//TODO: Debug code
		CheckFile();
	}

	/* (non-Javadoc)
	 * @see com.bottinifuel.EFT_File.Records.EFT_Record#dump()
	 */
	@Override
	public String dump()
	{
		String rc = "9";
		rc += String.format("%06d%06d%08d", BatchCount, BlockCount, EntryAddendaCount);
		rc += String.format("%010d", EntryHash.intValue());
		rc += String.format("%012d%012d", TotalDebits.movePointRight(2).intValue(), TotalCredits.movePointRight(2).intValue());
		rc += Data.substring(55);
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
